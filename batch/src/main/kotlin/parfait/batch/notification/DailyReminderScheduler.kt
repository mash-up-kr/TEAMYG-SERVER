package parfait.batch.notification

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import parfait.core.notification.domain.ReminderType
import java.time.LocalDate

@Component
class DailyReminderScheduler(
    private val jobOperator: JobOperator,
    private val dailyReminderJob: Job,
) {
    private val log = LoggerFactory.getLogger(DailyReminderScheduler::class.java)

    @Scheduled(cron = "\${notification.reminder.morning-cron:0 0 10 * * *}", zone = "Asia/Seoul")
    fun morning() = launch(ReminderType.MORNING)

    @Scheduled(cron = "\${notification.reminder.evening-cron:0 0 20 * * *}", zone = "Asia/Seoul")
    fun evening() = launch(ReminderType.EVENING)

    private fun launch(type: ReminderType) {
        val params =
            JobParametersBuilder()
                .addLocalDate("runDate", LocalDate.now())
                .addString("reminderType", type.name)
                .toJobParameters()
        runCatching { jobOperator.start(dailyReminderJob, params) }
            .onFailure { log.error("리마인드 배치 실행 실패 - type={}, params={}", type, params, it) }
    }
}
