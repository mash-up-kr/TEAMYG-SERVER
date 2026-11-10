package parfait.core.notification.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import parfait.core.notification.domain.TextPlacedPayload
import java.time.LocalDate
import java.time.LocalDateTime

class NotificationOutboxTest {
    private val now = LocalDateTime.of(2026, 9, 2, 10, 0, 0)
    private val payload =
        ToppingPlacedPayload(
            groupId = 50L,
            parfaitId = 123L,
            parfaitDate = LocalDate.of(2026, 9, 2),
            actorMemberId = 7L,
        )

    @Test
    fun `toppingPlaced 는 PENDING 상태와 규칙적인 dedupKey 로 생성한다`() {
        val row =
            NotificationOutbox.toppingPlaced(
                toppingId = 123L,
                receiverMemberId = 42L,
                payload = payload,
                now = now,
            )

        row.id shouldBe null
        row.aggregateType shouldBe "TOPPING"
        row.aggregateId shouldBe 123L
        row.eventType shouldBe "TOPPING_PLACED"
        row.receiverMemberId shouldBe 42L
        row.payload shouldBe payload
        row.dedupKey shouldBe "topping-placed:123:42"
        row.status shouldBe OutboxStatus.PENDING
        row.attempts shouldBe 0
        row.scheduledAt shouldBe now
        row.createdAt shouldBe now
        row.sentAt shouldBe null
        row.lastError shouldBe null
    }

    @Test
    fun `backgroundChanged 는 PARFAIT 집계와 변경시각이 들어간 dedupKey 로 생성한다`() {
        val changedAt = LocalDateTime.of(2026, 9, 2, 10, 5, 7, 123_000_000)
        val bgPayload =
            BackgroundChangedPayload(
                groupId = 50L,
                parfaitId = 123L,
                parfaitDate = LocalDate.of(2026, 9, 2),
                actorMemberId = 7L,
            )

        val row =
            NotificationOutbox.backgroundChanged(
                receiverMemberId = 42L,
                payload = bgPayload,
                changedAt = changedAt,
                now = now,
            )

        row.id shouldBe null
        row.aggregateType shouldBe "PARFAIT"
        row.aggregateId shouldBe 123L
        row.eventType shouldBe "BACKGROUND_CHANGED"
        row.receiverMemberId shouldBe 42L
        row.payload shouldBe bgPayload
        row.dedupKey shouldBe "background-changed:123:20260902100507123:42"
        row.status shouldBe OutboxStatus.PENDING
        row.attempts shouldBe 0
        row.scheduledAt shouldBe now
        row.createdAt shouldBe now
    }

    @Test
    fun `textPlaced 는 TEXT 집계와 규칙적인 dedupKey 로 생성한다`() {
        val textPayload =
            TextPlacedPayload(
                groupId = 50L,
                parfaitId = 123L,
                parfaitDate = LocalDate.of(2026, 9, 2),
                actorMemberId = 7L,
                text = "오늘 다들 뭐 먹었어",
            )

        val row =
            NotificationOutbox.textPlaced(
                textId = 77L,
                receiverMemberId = 42L,
                payload = textPayload,
                now = now,
            )

        row.id shouldBe null
        row.aggregateType shouldBe "TEXT"
        row.aggregateId shouldBe 77L
        row.eventType shouldBe "TEXT_PLACED"
        row.receiverMemberId shouldBe 42L
        row.payload shouldBe textPayload
        row.dedupKey shouldBe "text-placed:77:42"
        row.status shouldBe OutboxStatus.PENDING
        row.attempts shouldBe 0
        row.scheduledAt shouldBe now
        row.createdAt shouldBe now
        row.sentAt shouldBe null
        row.lastError shouldBe null
    }

    @Test
    fun `활동 묶음 예약 행은 윈도우 시작 시각이 dedupKey에 들어가고 종료 시각에 예약된다`() {
        val start = LocalDateTime.of(2026, 11, 3, 10, 0, 5)
        val end = LocalDateTime.of(2026, 11, 3, 10, 10, 5)
        val digestPayload =
            ActivityDigestPayload(
                groupId = 1L,
                parfaitId = 5L,
                parfaitDate = LocalDate.of(2026, 11, 3),
                actorMemberId = 7L,
            )

        val digest =
            NotificationOutbox.activityDigest(
                receiverMemberId = 42L,
                payload = digestPayload,
                windowStart = start,
                windowEnd = end,
            )

        digest.eventType shouldBe NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST
        digest.aggregateType shouldBe NotificationOutbox.AGGREGATE_TYPE_GROUP
        digest.aggregateId shouldBe 1L
        digest.receiverMemberId shouldBe 42L
        digest.dedupKey shouldBe "activity-digest:42:1:2026-11-03:20261103100005"
        digest.scheduledAt shouldBe end
        digest.createdAt shouldBe start
    }

    @Test
    fun `윈도우 열림 조회용 접두어는 dedupKey의 앞부분과 같다`() {
        NotificationOutbox.activityDigestKeyPrefix(42L, 1L, LocalDate.of(2026, 11, 3)) shouldBe
            "activity-digest:42:1:2026-11-03:"
    }

    @Test
    fun `윈도우 종료 시각은 10분 뒤이고 해당 날짜 캔버스 마감 03시를 넘지 않는다`() {
        val date = LocalDate.of(2026, 11, 3)

        NotificationOutbox.digestWindowEnd(LocalDateTime.of(2026, 11, 3, 10, 0), date) shouldBe
            LocalDateTime.of(2026, 11, 3, 10, 10)
        NotificationOutbox.digestWindowEnd(LocalDateTime.of(2026, 11, 4, 2, 55), date) shouldBe
            LocalDateTime.of(2026, 11, 4, 3, 0)
    }
}
