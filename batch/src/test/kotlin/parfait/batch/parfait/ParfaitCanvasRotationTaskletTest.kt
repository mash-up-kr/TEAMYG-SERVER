package parfait.batch.parfait

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import parfait.core.parfait.port.`in`.RotateParfaitCanvasesResult
import parfait.core.parfait.port.`in`.RotateParfaitCanvasesUseCase

class ParfaitCanvasRotationTaskletTest {
    private val rotateParfaitCanvasesUseCase = mockk<RotateParfaitCanvasesUseCase>()
    private val tasklet = ParfaitCanvasRotationTasklet(rotateParfaitCanvasesUseCase)

    @Test
    fun `UseCase를 호출하고 FINISHED를 반환한다`() {
        every { rotateParfaitCanvasesUseCase.rotateAll() } returns
            RotateParfaitCanvasesResult(closedCount = 2, emptyCount = 1, createdCount = 3, failedCount = 0)

        val result = tasklet.execute(mockk(relaxed = true), mockk(relaxed = true))

        result shouldBe RepeatStatus.FINISHED
        verify { rotateParfaitCanvasesUseCase.rotateAll() }
    }
}
