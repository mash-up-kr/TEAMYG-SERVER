package parfait.core.notification.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.notification.domain.FcmErrorCodes
import parfait.core.notification.domain.NotificationMessageFactory
import parfait.core.notification.domain.NotificationOutbox
import parfait.core.notification.domain.OutboxBackoff
import parfait.core.notification.domain.TextPlacedPayload
import parfait.core.notification.exception.NotificationSendException
import parfait.core.notification.port.`in`.OutboxBatchOutcome
import parfait.core.notification.port.`in`.ProcessNotificationOutboxUseCase
import parfait.core.notification.port.out.DeviceTokenDeletePort
import parfait.core.notification.port.out.DeviceTokenQueryPort
import parfait.core.notification.port.out.NotificationOutboxDigestPort
import parfait.core.notification.port.out.NotificationOutboxPollPort
import parfait.core.notification.port.out.NotificationSenderPort
import parfait.core.notification.port.out.NotificationSettingsQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupQueryPort
import java.time.LocalDateTime

/**
 * claim 한 배치를 `@Transactional` 안에서 순회하며 FCM 발송하고 결과를 마킹한다.
 *
 * 주의(락 보유 시간): `claimBatch` 는 최대 [CLAIM_BATCH_SIZE] 행을 `FOR UPDATE` 로 잡고, 그 락은
 * 이 트랜잭션이 끝날 때까지 유지된다. firebase-admin 9.9.0 은 모든 messaging 요청에
 * `DEFAULT_RETRY_CONFIG`(maxRetries=4, 503 재시도, `Retry-After` 최대 60s 준수)를 강제하며 이를 끄는
 * 공개 API 가 없다. 따라서 FCM 백엔드 장애 시 단 한 번의 `senderPort.send()` 가 최대 ~4분 블록될 수 있고,
 * 그동안 claim 한 행 락과 DB 커넥션을 계속 쥔다(스펙 §4 추정치 "~100~300ms" 와 크게 다름). FCM 장애가
 * 지속되면 스펙 §11 의 옵션 B(클레임 트랜잭션 밖 발송)로 전환한다.
 */
@Service
class NotificationOutboxDispatcher(
    private val pollPort: NotificationOutboxPollPort,
    private val groupQueryPort: ParfaitGroupQueryPort,
    private val groupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val deviceTokenQueryPort: DeviceTokenQueryPort,
    private val deviceTokenDeletePort: DeviceTokenDeletePort,
    private val senderPort: NotificationSenderPort,
    private val messageFactory: NotificationMessageFactory,
    private val coordinator: ActivityDigestCoordinator,
    private val settingsQueryPort: NotificationSettingsQueryPort,
    private val digestPort: NotificationOutboxDigestPort,
) : ProcessNotificationOutboxUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val CLAIM_BATCH_SIZE = 50
        const val CANCELLED_GROUP_DELETED = "CANCELLED_GROUP_DELETED"
        const val CANCELLED_RECEIVER_LEFT = "CANCELLED_RECEIVER_LEFT"
        const val NO_DEVICE_TOKEN = "NO_DEVICE_TOKEN"
        const val CANCELLED_EMPTY_PREVIEW = "CANCELLED_EMPTY_PREVIEW"
        const val CANCELLED_DIGEST_DISABLED = "CANCELLED_DIGEST_DISABLED"
        const val CANCELLED_EMPTY_DIGEST = "CANCELLED_EMPTY_DIGEST"
        const val DIGESTED = "DIGESTED"
        const val DIGEST_FAILED = "DIGEST_FAILED"
    }

    @Transactional
    override fun processDueBatch(now: LocalDateTime): OutboxBatchOutcome {
        val batch = pollPort.claimBatch(CLAIM_BATCH_SIZE, now)
        var sent = 0
        var cancelled = 0
        var retried = 0
        var failed = 0
        var deferred = 0
        val cancelledByReason = mutableMapOf<String, Int>()

        fun cancel(
            row: NotificationOutbox,
            reason: String,
        ) {
            pollPort.markSent(requireNotNull(row.id), now, reason)
            if (row.eventType == NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST) {
                digestPort.resolveDeferred(row.dedupKey, now, reason)
            }
            cancelled++
            cancelledByReason.merge(reason, 1, Int::plus)
        }

        for (row in batch) {
            val id = requireNotNull(row.id)
            val p = row.payload

            val group = groupQueryPort.findById(p.groupId)
            if (group == null) { // E-04
                cancel(row, CANCELLED_GROUP_DELETED)
                continue
            }

            val receiver = groupMemberQueryPort.findByGroupIdAndMemberId(p.groupId, row.receiverMemberId)
            if (receiver == null || receiver.leftAt != null) { // E-03
                cancel(row, CANCELLED_RECEIVER_LEFT)
                continue
            }

            val actor = groupMemberQueryPort.findByGroupIdAndMemberId(p.groupId, p.actorMemberId)
            val actorNickname = actor?.takeIf { it.leftAt == null }?.groupNickname?.value // E-05

            val tokens = deviceTokenQueryPort.findByMemberId(row.receiverMemberId)
            if (tokens.isEmpty()) { // E-02
                cancel(row, NO_DEVICE_TOKEN)
                continue
            }

            // 빈 미리보기는 알림이 나가지 않으므로 decide(P-06 윈도우 생성) 보다 먼저 취소한다.
            val textPreview =
                if (row.eventType == NotificationOutbox.EVENT_TYPE_TEXT_PLACED) {
                    messageFactory.generateTextPreview((p as TextPlacedPayload).text)
                } else {
                    null
                }
            if (row.eventType == NotificationOutbox.EVENT_TYPE_TEXT_PLACED && textPreview == null) {
                cancel(row, CANCELLED_EMPTY_PREVIEW)
                continue
            }

            if (row.eventType != NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST &&
                coordinator.decide(row, now) == DigestDecision.DEFER
            ) {
                deferred++
                continue
            }

            val message =
                when (row.eventType) {
                    NotificationOutbox.EVENT_TYPE_TOPPING_PLACED ->
                        messageFactory.toppingPlaced(group.name.value, actorNickname, p.groupId, p.parfaitDate)
                    NotificationOutbox.EVENT_TYPE_BACKGROUND_CHANGED ->
                        messageFactory.backgroundChanged(group.name.value, actorNickname, p.groupId, p.parfaitDate)
                    NotificationOutbox.EVENT_TYPE_TEXT_PLACED ->
                        messageFactory.textPlaced(
                            group.name.value,
                            actorNickname,
                            p.groupId,
                            p.parfaitDate,
                            requireNotNull(textPreview),
                        )
                    NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST -> {
                        if (!settingsQueryPort.findByMemberId(row.receiverMemberId).activityDigestEnabled) {
                            cancel(row, CANCELLED_DIGEST_DISABLED) // E-27
                            continue
                        }
                        val count = coordinator.countAlive(row.dedupKey)
                        if (count == 0) {
                            cancel(row, CANCELLED_EMPTY_DIGEST) // E-26
                            continue
                        }
                        messageFactory.activityDigest(group.name.value, count, p.groupId, p.parfaitDate) // E-28
                    }
                    else -> {
                        // 예외를 던지면 배치 트랜잭션 전체가 롤백되므로 해당 행만 종료 처리하고 계속한다.
                        pollPort.markFailed(id, "UNKNOWN_EVENT_TYPE: ${row.eventType}")
                        log.warn("outbox 알 수 없는 eventType id={} eventType={}", id, row.eventType)
                        failed++
                        continue
                    }
                }

            var anySuccess = false
            var anyRetryable = false
            var lastError: String? = null
            for (t in tokens) {
                runCatching { senderPort.send(t.token, message) }
                    .onSuccess { anySuccess = true }
                    .onFailure { e ->
                        val ex = e as? NotificationSendException
                        lastError = "${ex?.errorCode ?: e::class.simpleName}: ${e.message}".take(500)
                        when {
                            // E-10/E-12
                            ex?.errorCode in FcmErrorCodes.DEAD_TOKEN -> deviceTokenDeletePort.deleteByToken(t.token)
                            ex == null || ex.retryable -> anyRetryable = true
                        }
                    }
            }

            when {
                anySuccess -> {
                    pollPort.markSent(id, now, null)
                    if (row.eventType == NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST) {
                        digestPort.resolveDeferred(row.dedupKey, now, DIGESTED)
                    }
                    sent++
                }
                anyRetryable && row.attempts + 1 < OutboxBackoff.MAX_ATTEMPTS -> {
                    val next = row.attempts + 1
                    pollPort.markRetry(
                        id,
                        next,
                        now.plus(OutboxBackoff.nextDelay(next)),
                        lastError ?: "retryable send failure",
                    )
                    log.warn("outbox 재시도 예약 id={} attempts={} error={}", id, next, lastError)
                    retried++
                }
                else -> {
                    pollPort.markFailed(id, lastError ?: "all tokens failed")
                    if (row.eventType == NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST) {
                        digestPort.resolveDeferred(row.dedupKey, now, DIGEST_FAILED)
                    }
                    log.warn("outbox 발송 실패(종료) id={} error={}", id, lastError)
                    failed++
                }
            }
        }

        if (batch.isNotEmpty()) {
            log.info(
                "outbox 배치 처리 - claimed={} sent={} deferred={} cancelled={} retried={} failed={} byReason={}",
                batch.size,
                sent,
                deferred,
                cancelled,
                retried,
                failed,
                cancelledByReason,
            )
        }

        return OutboxBatchOutcome(
            claimed = batch.size,
            sent = sent,
            cancelled = cancelled,
            retried = retried,
            failed = failed,
            cancelledByReason = cancelledByReason,
        )
    }
}
