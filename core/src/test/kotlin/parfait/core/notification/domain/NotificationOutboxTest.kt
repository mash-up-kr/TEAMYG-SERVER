package parfait.core.notification.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
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
}
