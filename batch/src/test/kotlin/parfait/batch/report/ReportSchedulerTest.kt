package parfait.batch.report

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.report.domain.ReportSendOutcome
import parfait.core.report.port.`in`.SendScheduledReportUseCase

class ReportSchedulerTest {
    private val useCase = mockk<SendScheduledReportUseCase>()
    private val scheduler = ReportScheduler(useCase)

    @Test
    fun `실행하면 UseCase 를 호출한다`() {
        every { useCase.sendIfDue(any()) } returns ReportSendOutcome.SENT

        scheduler.run()

        verify(exactly = 1) { useCase.sendIfDue(any()) }
    }

    @Test
    fun `UseCase 가 예외를 던져도 스케줄러 밖으로 전파하지 않는다`() {
        every { useCase.sendIfDue(any()) } throws IllegalStateException("DB 오류")

        scheduler.run()

        verify(exactly = 1) { useCase.sendIfDue(any()) }
    }
}
