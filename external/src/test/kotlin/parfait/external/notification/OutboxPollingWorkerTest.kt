package parfait.external.notification

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.notification.port.`in`.OutboxBatchOutcome
import parfait.core.notification.port.`in`.ProcessNotificationOutboxUseCase

class OutboxPollingWorkerTest {
    private val useCase = mockk<ProcessNotificationOutboxUseCase>()
    private val worker = OutboxPollingWorker(useCase)

    private fun outcome(claimed: Int) = OutboxBatchOutcome(claimed, claimed, 0, 0, 0)

    @Test
    fun `drain 은 claimed 가 0 이 될 때까지 반복 호출한다`() {
        every { useCase.processDueBatch(any()) } returnsMany listOf(outcome(50), outcome(50), outcome(0))

        worker.drain()

        verify(exactly = 3) { useCase.processDueBatch(any()) }
    }

    @Test
    fun `drain 은 안전 상한(20회)을 넘지 않는다`() {
        every { useCase.processDueBatch(any()) } returns outcome(50)

        worker.drain()

        verify(exactly = 20) { useCase.processDueBatch(any()) }
    }

    @Test
    fun `drain 은 예외를 삼킨다`() {
        every { useCase.processDueBatch(any()) } throws RuntimeException("boom")

        worker.drain() // 예외가 전파되지 않아야 함
    }
}
