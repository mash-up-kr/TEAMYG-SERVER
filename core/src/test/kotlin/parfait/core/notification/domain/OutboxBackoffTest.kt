package parfait.core.notification.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration

class OutboxBackoffTest {
    @Test
    fun `1~5회차 백오프 간격`() {
        OutboxBackoff.nextDelay(1) shouldBe Duration.ofMinutes(1)
        OutboxBackoff.nextDelay(2) shouldBe Duration.ofMinutes(5)
        OutboxBackoff.nextDelay(3) shouldBe Duration.ofMinutes(15)
        OutboxBackoff.nextDelay(4) shouldBe Duration.ofHours(1)
        OutboxBackoff.nextDelay(5) shouldBe Duration.ofHours(6)
    }

    @Test
    fun `스케줄을 넘는 회차는 마지막 간격을 반복한다`() {
        OutboxBackoff.nextDelay(6) shouldBe Duration.ofHours(6)
        OutboxBackoff.nextDelay(99) shouldBe Duration.ofHours(6)
    }
}
