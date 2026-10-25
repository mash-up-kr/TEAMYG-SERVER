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
}
