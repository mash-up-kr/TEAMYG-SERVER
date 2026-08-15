package parfait.batch.parfait

import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.stereotype.Component
import parfait.core.parfait.port.`in`.RotateParfaitCanvasesUseCase

@Component
class ParfaitCanvasRotationTasklet(
    private val rotateParfaitCanvasesUseCase: RotateParfaitCanvasesUseCase,
) : Tasklet {
    private val log = LoggerFactory.getLogger(ParfaitCanvasRotationTasklet::class.java)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val result = rotateParfaitCanvasesUseCase.rotateAll()
        log.info(
            "캔버스 회전 완료 - closed={}, empty={}, created={}, failed={}",
            result.closedCount,
            result.emptyCount,
            result.createdCount,
            result.failedCount,
        )
        return RepeatStatus.FINISHED
    }
}
