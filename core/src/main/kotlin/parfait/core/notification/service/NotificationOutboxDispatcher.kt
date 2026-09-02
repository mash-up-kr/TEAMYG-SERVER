package parfait.core.notification.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    private companion object {
        const val CLAIM_BATCH_SIZE = 50
        val DEAD_TOKEN_CODES = setOf("UNREGISTERED", "INVALID_ARGUMENT", "SENDER_ID_MISMATCH")
    }

    @Transactional
    override fun processDueBatch(now: LocalDateTime): OutboxBatchOutcome {
        val batch = pollPort.claimBatch(CLAIM_BATCH_SIZE, now)
        var sent = 0
        var cancelled = 0
        var retried = 0
        var failed = 0

        for (row in batch) {
            val id = requireNotNull(row.id)
            val p = row.payload

            val group = groupQueryPort.findById(p.groupId)
            if (group == null) { // E-04
                pollPort.markSent(id, now, "CANCELLED_GROUP_DELETED")
                cancelled++
                continue
            }

            val receiver = groupMemberQueryPort.findByGroupIdAndMemberId(p.groupId, row.receiverMemberId)
            if (receiver == null || receiver.leftAt != null) { // E-03
                pollPort.markSent(id, now, "CANCELLED_RECEIVER_LEFT")
                cancelled++
                continue
            }

            val actor = groupMemberQueryPort.findByGroupIdAndMemberId(p.groupId, p.actorMemberId)
            val actorNickname = actor?.takeIf { it.leftAt == null }?.groupNickname?.value // E-05

            val tokens = deviceTokenQueryPort.findByMemberId(row.receiverMemberId)
            if (tokens.isEmpty()) { // E-02
                pollPort.markSent(id, now, "NO_DEVICE_TOKEN")
                cancelled++
                continue
            }

            val message = messageFactory.toppingPlaced(group.name.value, actorNickname, p.groupId, p.parfaitDate)

            var anySuccess = false
            var anyRetryable = false
            for (t in tokens) {
                runCatching { senderPort.send(t.token, message) }
                    .onSuccess { anySuccess = true }
                    .onFailure { e ->
                        val ex = e as? NotificationSendException
                        when {
                            // E-10/E-12
                            ex?.errorCode in DEAD_TOKEN_CODES -> deviceTokenDeletePort.deleteByToken(t.token)
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
                    pollPort.markRetry(id, next, now.plus(OutboxBackoff.nextDelay(next)), "retryable send failure")
                    retried++
                }
                else -> {
                    pollPort.markFailed(id, "all tokens failed")
                    failed++
                }
            }
        }

        return OutboxBatchOutcome(
            claimed = batch.size,
            sent = sent,
            cancelled = cancelled,
            retried = retried,
            failed = failed,
        )
    }
}
