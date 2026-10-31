package parfait.batch.report

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import parfait.core.report.port.`in`.SendScheduledReportUseCase
import java.time.LocalDateTime
import java.time.ZoneId

@Component
@ConditionalOnProperty(prefix = "report", name = ["enabled"], havingValue = "true")
class ReportScheduler(
    private val sendScheduledReportUseCase: SendScheduledReportUseCase,
) {
    private val log = LoggerFactory.getLogger(ReportScheduler::class.java)

    @Scheduled(cron = "\${report.cron:0 0 21 * * *}", zone = "Asia/Seoul")
    fun run() {
        runCatching { sendScheduledReportUseCase.sendIfDue(LocalDateTime.now(KST)) }
            .onSuccess { log.info("정기 리포트 실행 결과: {}", it) }
            .onFailure { log.error("정기 리포트 실행 실패", it) }
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
