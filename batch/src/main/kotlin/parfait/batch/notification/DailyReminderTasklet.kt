package parfait.batch.notification

import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.stereotype.Component
import parfait.core.notification.domain.ReminderType
import parfait.core.notification.port.`in`.SendDailyReminderUseCase

@Component
class DailyReminderTasklet(
    private val sendDailyReminderUseCase: SendDailyReminderUseCase,
) : Tasklet {
    private val log = LoggerFactory.getLogger(DailyReminderTasklet::class.java)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val type = ReminderType.valueOf(chunkContext.stepContext.jobParameters["reminderType"] as String)
        val outcome = sendDailyReminderUseCase.send(type)
        log.info(
            "리마인드 발송 완료 - type={}, skipped={}, 대상={}, 성공={}, 실패={}, 죽은토큰삭제={}",
            type,
            outcome.skipped,
            outcome.targeted,
            outcome.sent,
            outcome.failed,
            outcome.deadTokensDeleted,
        )
        return RepeatStatus.FINISHED
    }
}
