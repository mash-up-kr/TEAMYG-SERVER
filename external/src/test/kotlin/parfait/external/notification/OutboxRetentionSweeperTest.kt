package parfait.external.notification

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.notification.port.`in`.PurgeNotificationOutboxUseCase
import java.time.Duration

class OutboxRetentionSweeperTest {
    private val useCase = mockk<PurgeNotificationOutboxUseCase>(relaxed = true)
    private val sweeper = OutboxRetentionSweeper(useCase, retentionDays = 7)

    @Test
    fun `retention-days 로 purgeTerminal 을 호출한다`() {
        every { useCase.purgeTerminal(any(), any()) } returns 3

        sweeper.sweep()

        verify { useCase.purgeTerminal(retention = Duration.ofDays(7), now = any()) }
    }

    @Test
    fun `purge 가 실패해도 예외를 삼킨다`() {
        every { useCase.purgeTerminal(any(), any()) } throws RuntimeException("boom")

        sweeper.sweep()
    }
}
