package parfait.batch.parfait

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class ParfaitCanvasRotationJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val parfaitCanvasRotationTasklet: ParfaitCanvasRotationTasklet,
) {
    @Bean
    fun parfaitCanvasRotationStep(): Step =
        StepBuilder("parfaitCanvasRotationStep", jobRepository)
            .tasklet(parfaitCanvasRotationTasklet, transactionManager)
            .build()

    @Bean
    fun parfaitCanvasRotationJob(): Job =
        JobBuilder("parfaitCanvasRotationJob", jobRepository)
            .start(parfaitCanvasRotationStep())
            .build()
}
