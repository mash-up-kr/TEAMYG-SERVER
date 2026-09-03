package parfait.batch.notification

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class DailyReminderJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val dailyReminderTasklet: DailyReminderTasklet,
) {
    @Bean
    fun dailyReminderStep(): Step =
        StepBuilder("dailyReminderStep", jobRepository)
            .tasklet(dailyReminderTasklet, transactionManager)
            .build()

    @Bean
    fun dailyReminderJob(): Job =
        JobBuilder("dailyReminderJob", jobRepository)
            .start(dailyReminderStep())
            .build()
}
