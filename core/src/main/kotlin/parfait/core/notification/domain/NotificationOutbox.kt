package parfait.core.notification.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    val payload: NotificationPayload,
    val dedupKey: String,
    val status: OutboxStatus,
    val attempts: Int,
    val scheduledAt: LocalDateTime,
    val lastError: String?,
    val createdAt: LocalDateTime,
    val sentAt: LocalDateTime?,
    val digestRef: String?,
) {
    companion object {
        const val AGGREGATE_TYPE_TOPPING = "TOPPING"
        const val EVENT_TYPE_TOPPING_PLACED = "TOPPING_PLACED"
        const val AGGREGATE_TYPE_PARFAIT = "PARFAIT"
        const val EVENT_TYPE_BACKGROUND_CHANGED = "BACKGROUND_CHANGED"
        const val AGGREGATE_TYPE_TEXT = "TEXT"
        const val EVENT_TYPE_TEXT_PLACED = "TEXT_PLACED"

        const val AGGREGATE_TYPE_GROUP = "GROUP"
        const val EVENT_TYPE_ACTIVITY_DIGEST = "ACTIVITY_DIGEST"

        private const val DIGEST_KEY_TIME_LENGTH = 14
        private val DIGEST_WINDOW: Duration = Duration.ofMinutes(10)
        private val CANVAS_ROLLOVER: LocalTime = LocalTime.of(3, 0)

        private val DEDUP_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")

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
                digestRef = null,
            )

        fun backgroundChanged(
            receiverMemberId: Long,
            payload: BackgroundChangedPayload,
            changedAt: LocalDateTime,
            now: LocalDateTime = LocalDateTime.now(),
        ): NotificationOutbox {
            val changedAtKey = changedAt.format(DEDUP_TIME_FORMAT)
            val dedupKey = "background-changed:${payload.parfaitId}:$changedAtKey:$receiverMemberId"
            return NotificationOutbox(
                id = null,
                aggregateType = AGGREGATE_TYPE_PARFAIT,
                aggregateId = payload.parfaitId,
                eventType = EVENT_TYPE_BACKGROUND_CHANGED,
                receiverMemberId = receiverMemberId,
                payload = payload,
                dedupKey = dedupKey,
                status = OutboxStatus.PENDING,
                attempts = 0,
                scheduledAt = now,
                lastError = null,
                createdAt = now,
                sentAt = null,
                digestRef = null,
            )
        }

        fun textPlaced(
            textId: Long,
            receiverMemberId: Long,
            payload: TextPlacedPayload,
            now: LocalDateTime = LocalDateTime.now(),
        ): NotificationOutbox =
            NotificationOutbox(
                id = null,
                aggregateType = AGGREGATE_TYPE_TEXT,
                aggregateId = textId,
                eventType = EVENT_TYPE_TEXT_PLACED,
                receiverMemberId = receiverMemberId,
                payload = payload,
                dedupKey = "text-placed:$textId:$receiverMemberId",
                status = OutboxStatus.PENDING,
                attempts = 0,
                scheduledAt = now,
                lastError = null,
                createdAt = now,
                sentAt = null,
                digestRef = null,
            )

        fun activityDigestKeyPrefix(
            receiverMemberId: Long,
            groupId: Long,
            parfaitDate: LocalDate,
        ): String = "activity-digest:$receiverMemberId:$groupId:$parfaitDate:"

        /** 윈도우 종료 시각 = 시작 + 10분. 단 해당 날짜 캔버스의 마감(다음 날 03:00)을 넘기지 않는다(E-24). */
        fun digestWindowEnd(
            windowStart: LocalDateTime,
            parfaitDate: LocalDate,
        ): LocalDateTime {
            val canvasClose = parfaitDate.plusDays(1).atTime(CANVAS_ROLLOVER)
            val end = windowStart.plus(DIGEST_WINDOW)
            return if (end.isAfter(canvasClose)) canvasClose else end
        }

        fun activityDigest(
            receiverMemberId: Long,
            payload: ActivityDigestPayload,
            windowStart: LocalDateTime,
            windowEnd: LocalDateTime,
        ): NotificationOutbox =
            NotificationOutbox(
                id = null,
                aggregateType = AGGREGATE_TYPE_GROUP,
                aggregateId = payload.groupId,
                eventType = EVENT_TYPE_ACTIVITY_DIGEST,
                receiverMemberId = receiverMemberId,
                payload = payload,
                dedupKey =
                    activityDigestKeyPrefix(receiverMemberId, payload.groupId, payload.parfaitDate) +
                        windowStart.format(DEDUP_TIME_FORMAT).take(DIGEST_KEY_TIME_LENGTH),
                status = OutboxStatus.PENDING,
                attempts = 0,
                scheduledAt = windowEnd,
                lastError = null,
                createdAt = windowStart,
                sentAt = null,
                digestRef = null,
            )

        @Suppress("LongParameterList")
        fun reconstitute(
            id: Long,
            aggregateType: String,
            aggregateId: Long,
            eventType: String,
            receiverMemberId: Long,
            payload: NotificationPayload,
            dedupKey: String,
            status: OutboxStatus,
            attempts: Int,
            scheduledAt: LocalDateTime,
            lastError: String?,
            createdAt: LocalDateTime,
            sentAt: LocalDateTime?,
            digestRef: String? = null,
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
                digestRef,
            )
    }
}
