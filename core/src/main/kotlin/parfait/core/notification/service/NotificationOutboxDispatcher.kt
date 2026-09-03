package parfait.core.notification.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.notification.domain.FcmErrorCodes
import parfait.core.notification.domain.NotificationMessageFactory
import parfait.core.notification.domain.OutboxBackoff
import parfait.core.notification.exception.NotificationSendException
import parfait.core.notification.port.`in`.OutboxBatchOutcome
import parfait.core.notification.port.`in`.ProcessNotificationOutboxUseCase
import parfait.core.notification.port.out.DeviceTokenDeletePort
import parfait.core.notification.port.out.DeviceTokenQueryPort
import parfait.core.notification.port.out.NotificationOutboxPollPort
import parfait.core.notification.port.out.NotificationSenderPort
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
) : ProcessNotificationOutboxUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val CLAIM_BATCH_SIZE = 50
        const val CANCELLED_GROUP_DELETED = "CANCELLED_GROUP_DELETED"
        const val CANCELLED_RECEIVER_LEFT = "CANCELLED_RECEIVER_LEFT"
        const val NO_DEVICE_TOKEN = "NO_DEVICE_TOKEN"
    }

    @Transactional
    override fun processDueBatch(now: LocalDateTime): OutboxBatchOutcome {
        val batch = pollPort.claimBatch(CLAIM_BATCH_SIZE, now)
        var sent = 0
        var cancelled = 0
        var retried = 0
        var failed = 0
        val cancelledByReason = mutableMapOf<String, Int>()

        fun cancel(
            id: Long,
            reason: String,
        ) {
            pollPort.markSent(id, now, reason)
            cancelled++
            cancelledByReason.merge(reason, 1, Int::plus)
        }

        for (row in batch) {
            val id = requireNotNull(row.id)
            val p = row.payload

            val group = groupQueryPort.findById(p.groupId)
            if (group == null) { // E-04
                cancel(id, CANCELLED_GROUP_DELETED)
                continue
            }

            val receiver = groupMemberQueryPort.findByGroupIdAndMemberId(p.groupId, row.receiverMemberId)
            if (receiver == null || receiver.leftAt != null) { // E-03
                cancel(id, CANCELLED_RECEIVER_LEFT)
                continue
            }

            val actor = groupMemberQueryPort.findByGroupIdAndMemberId(p.groupId, p.actorMemberId)
            val actorNickname = actor?.takeIf { it.leftAt == null }?.groupNickname?.value // E-05

            val tokens = deviceTokenQueryPort.findByMemberId(row.receiverMemberId)
            if (tokens.isEmpty()) { // E-02
                cancel(id, NO_DEVICE_TOKEN)
                continue
            }

            val message = messageFactory.toppingPlaced(group.name.value, actorNickname, p.groupId, p.parfaitDate)

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
                    log.warn("outbox 발송 실패(종료) id={} error={}", id, lastError)
                    failed++
                }
            }
        }

        if (batch.isNotEmpty()) {
            log.info(
                "outbox 배치 처리 - claimed={} sent={} cancelled={} retried={} failed={} byReason={}",
                batch.size,
                sent,
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
