package parfait.core.notification.domain

import java.time.LocalDateTime

/**
 * notification_outbox 한 행의 도메인 표현. 상태 전이는 이 객체가 아니라
 * NotificationOutboxPollPort 의 UPDATE 메서드가 담당한다(옵션 A: 클레임 트랜잭션 안에서 처리).
 */
class NotificationOutbox private constructor(
    val id: Long?,
    val aggregateType: String,
    val aggregateId: Long,
    val eventType: String,
    val receiverMemberId: Long,
    val payload: ToppingPlacedPayload,
    val dedupKey: String,
    val status: OutboxStatus,
    val attempts: Int,
    val scheduledAt: LocalDateTime,
    val lastError: String?,
    val createdAt: LocalDateTime,
    val sentAt: LocalDateTime?,
) {
    companion object {
        const val AGGREGATE_TYPE_TOPPING = "TOPPING"
        const val EVENT_TYPE_TOPPING_PLACED = "TOPPING_PLACED"

        fun toppingPlaced(
            toppingId: Long,
            receiverMemberId: Long,
            payload: ToppingPlacedPayload,
            now: LocalDateTime = LocalDateTime.now(),
        ): NotificationOutbox =
            NotificationOutbox(
                id = null,
                aggregateType = AGGREGATE_TYPE_TOPPING,
                aggregateId = toppingId,
                eventType = EVENT_TYPE_TOPPING_PLACED,
                receiverMemberId = receiverMemberId,
                payload = payload,
                dedupKey = "topping-placed:$toppingId:$receiverMemberId",
                status = OutboxStatus.PENDING,
                attempts = 0,
                scheduledAt = now,
                lastError = null,
                createdAt = now,
                sentAt = null,
            )

        @Suppress("LongParameterList")
        fun reconstitute(
            id: Long,
            aggregateType: String,
            aggregateId: Long,
            eventType: String,
            receiverMemberId: Long,
            payload: ToppingPlacedPayload,
            dedupKey: String,
            status: OutboxStatus,
            attempts: Int,
            scheduledAt: LocalDateTime,
            lastError: String?,
            createdAt: LocalDateTime,
            sentAt: LocalDateTime?,
        ): NotificationOutbox =
            NotificationOutbox(
                id,
                aggregateType,
                aggregateId,
                eventType,
                receiverMemberId,
                payload,
                dedupKey,
                status,
                attempts,
                scheduledAt,
                lastError,
                createdAt,
                sentAt,
            )
    }
}
