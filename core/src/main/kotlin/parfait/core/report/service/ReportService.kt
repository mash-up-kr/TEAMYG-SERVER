package parfait.core.report.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import parfait.core.report.domain.ReportContent
import parfait.core.report.domain.ReportPeriod
import parfait.core.report.domain.ReportPlanner
import parfait.core.report.domain.ReportSendOutcome
import parfait.core.report.port.`in`.BuildReportNowUseCase
import parfait.core.report.port.`in`.SendScheduledReportUseCase
import parfait.core.report.port.out.ReportSendPort
import parfait.core.report.port.out.ReportSettingsQueryPort
import parfait.core.report.port.out.ReportSettingsSavePort
import parfait.core.stats.port.`in`.GetSignupStatsComparisonUseCase
import java.time.LocalDateTime

@Service
class ReportService(
    private val settingsQueryPort: ReportSettingsQueryPort,
    private val settingsSavePort: ReportSettingsSavePort,
    private val getSignupStatsComparisonUseCase: GetSignupStatsComparisonUseCase,
    private val reportSendPort: ReportSendPort,
    private val planner: ReportPlanner,
    // 테스트에서 0 을 넘겨 슬립을 건너뛴다. @Value 아님 — 기본값 상수.
    private val retryDelayMillis: Long = RETRY_DELAY_MS,
) : SendScheduledReportUseCase,
    BuildReportNowUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun buildNow(now: LocalDateTime): ReportContent =
        buildContent(planner.planNow(settingsQueryPort.find(), now))

    override fun sendIfDue(now: LocalDateTime): ReportSendOutcome {
        val period = planner.plan(settingsQueryPort.find(), now) ?: return ReportSendOutcome.SKIPPED
        val content = buildContent(period)

        if (!sendWithOneRetry(content)) return ReportSendOutcome.FAILED

        settingsSavePort.saveLastSentAt(now)
        return ReportSendOutcome.SENT
    }

    private fun buildContent(period: ReportPeriod): ReportContent {
        val comparison = getSignupStatsComparisonUseCase.getComparison(period.from, period.to)
        return ReportContent(period, comparison.current, comparison.growth)
    }

    private fun sendWithOneRetry(content: ReportContent): Boolean {
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                reportSendPort.send(content)
                return true
            } catch (e: Exception) {
                log.warn("리포트 전송 실패 ({}/{}): {}", attempt + 1, MAX_ATTEMPTS, e.message)
                if (attempt + 1 < MAX_ATTEMPTS && !pauseBeforeRetry()) return false
            }
        }
        // ERROR 로그는 DiscordErrorAppender 가 #errors 로 보낸다.
        log.error("리포트 전송 최종 실패 - period={}", content.period)
        return false
    }

    private fun pauseBeforeRetry(): Boolean {
        if (retryDelayMillis <= 0) return true
        return try {
            Thread.sleep(retryDelayMillis)
            true
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 2
        private const val RETRY_DELAY_MS = 60_000L
    }
}
