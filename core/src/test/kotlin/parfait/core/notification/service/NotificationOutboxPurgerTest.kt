package parfait.core.notification.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.notification.port.out.NotificationOutboxPurgePort
import java.time.Duration
import java.time.LocalDateTime

class NotificationOutboxPurgerTest {
    private val purgePort = mockk<NotificationOutboxPurgePort>()
    private val purger = NotificationOutboxPurger(purgePort)

    @Test
    fun `now 에서 retention 을 뺀 시각 이전 종료 행을 삭제하고 삭제 건수를 반환한다`() {
        val now = LocalDateTime.of(2026, 9, 10, 4, 0)
        every { purgePort.deleteTerminalBefore(now.minusDays(7)) } returns 123

        val deleted = purger.purgeTerminal(Duration.ofDays(7), now)

        deleted shouldBe 123
        verify { purgePort.deleteTerminalBefore(LocalDateTime.of(2026, 9, 3, 4, 0)) }
    }
}
