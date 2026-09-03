package parfait.batch.notification

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.scope.context.StepContext
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import parfait.core.notification.domain.ReminderType
import parfait.core.notification.port.`in`.ReminderSendOutcome
import parfait.core.notification.port.`in`.SendDailyReminderUseCase

class DailyReminderTaskletTest {
    private val useCase = mockk<SendDailyReminderUseCase>()
    private val tasklet = DailyReminderTasklet(useCase)

    private fun chunkContextWith(reminderType: String): ChunkContext {
        val stepContext = mockk<StepContext>()
        every { stepContext.jobParameters } returns mapOf<String, Any>("reminderType" to reminderType)
        val chunkContext = mockk<ChunkContext>()
        every { chunkContext.stepContext } returns stepContext
        return chunkContext
    }

    @Test
    fun `jobParameters 의 reminderType 으로 UseCase 를 호출하고 FINISHED 를 반환한다`() {
        val typeSlot = slot<ReminderType>()
        every { useCase.send(capture(typeSlot), any()) } returns
            ReminderSendOutcome(skipped = false, targeted = 10, sent = 9, failed = 1, deadTokensDeleted = 0)

        val result = tasklet.execute(mockk(relaxed = true), chunkContextWith("EVENING"))

        result shouldBe RepeatStatus.FINISHED
        typeSlot.captured shouldBe ReminderType.EVENING
        verify { useCase.send(ReminderType.EVENING, any()) }
    }
}
