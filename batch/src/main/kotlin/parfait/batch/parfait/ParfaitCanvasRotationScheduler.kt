package parfait.batch.parfait

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ParfaitCanvasRotationScheduler(
    private val jobOperator: JobOperator,
    private val parfaitCanvasRotationJob: Job,
) {
    private val log = LoggerFactory.getLogger(ParfaitCanvasRotationScheduler::class.java)

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    fun rotate() {
        val params =
            JobParametersBuilder()
                .addLocalDate("runDate", LocalDate.now())
                .toJobParameters()
        runCatching { jobOperator.start(parfaitCanvasRotationJob, params) }
            .onFailure { log.error("캔버스 회전 배치 실행 실패 - params={}", params, it) }
    }
}
