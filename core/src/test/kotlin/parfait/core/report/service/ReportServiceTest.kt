package parfait.core.report.service

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.report.domain.ReportContent
import parfait.core.report.domain.ReportPeriod
import parfait.core.report.domain.ReportPlanner
import parfait.core.report.domain.ReportSendOutcome
import parfait.core.report.domain.ReportSettings
import parfait.core.report.port.out.ReportSendPort
import parfait.core.report.port.out.ReportSettingsQueryPort
import parfait.core.report.port.out.ReportSettingsSavePort
import parfait.core.stats.domain.SignupGrowth
import parfait.core.stats.domain.SignupStats
import parfait.core.stats.domain.SignupStatsComparison
import parfait.core.stats.port.`in`.GetSignupStatsComparisonUseCase
import java.time.LocalDateTime

class ReportServiceTest {
    private val settingsQueryPort = mockk<ReportSettingsQueryPort>()
    private val settingsSavePort = mockk<ReportSettingsSavePort>(relaxed = true)
    private val getSignupStatsComparisonUseCase = mockk<GetSignupStatsComparisonUseCase>()
    private val reportSendPort = mockk<ReportSendPort>()
    private val service =
        ReportService(
            settingsQueryPort,
            settingsSavePort,
            getSignupStatsComparisonUseCase,
            reportSendPort,
            ReportPlanner(),
            retryDelayMillis = 0L,
        )

    private val now = LocalDateTime.of(2026, 10, 30, 21, 0, 0)
    private val stats = SignupStats(newSignups = 42, withdrawals = 3, newSignupWithdrawals = 1, totalUsers = 1284)
    private val period = ReportPeriod(now.minusDays(3), now)
    private val previousStats =
        SignupStats(newSignups = 40, withdrawals = 2, newSignupWithdrawals = 0, totalUsers = 1245)
    private val comparison = SignupStatsComparison(stats, previousStats)
    private val expectedContent = ReportContent(period, stats, SignupGrowth.between(stats, previousStats))

    private fun due() {
        every { settingsQueryPort.find() } returns ReportSettings(adMode = false, intervalDays = 3, lastSentAt = null)
        every { getSignupStatsComparisonUseCase.getComparison(period.from, period.to) } returns comparison
    }

    @Test
    fun `전송할 때가 아니면 통계 조회도 전송도 하지 않는다`() {
        every { settingsQueryPort.find() } returns
            ReportSettings(adMode = false, intervalDays = 3, lastSentAt = now.minusDays(1))

        service.sendIfDue(now) shouldBe ReportSendOutcome.SKIPPED

        verify(exactly = 0) { getSignupStatsComparisonUseCase.getComparison(any(), any()) }
        verify(exactly = 0) { reportSendPort.send(any()) }
        verify(exactly = 0) { settingsSavePort.saveLastSentAt(any()) }
    }

    @Test
    fun `전송에 성공하면 마지막 전송 시각을 저장한다`() {
        due()
        every { reportSendPort.send(expectedContent) } just Runs

        service.sendIfDue(now) shouldBe ReportSendOutcome.SENT

        verify(exactly = 1) { reportSendPort.send(expectedContent) }
        verify(exactly = 1) { settingsSavePort.saveLastSentAt(now) }
    }

    @Test
    fun `첫 전송이 실패해도 한 번 재시도해서 성공하면 SENT다`() {
        due()
        every { reportSendPort.send(expectedContent) } throws IllegalStateException("디스코드 오류") andThen Unit

        service.sendIfDue(now) shouldBe ReportSendOutcome.SENT

        verify(exactly = 2) { reportSendPort.send(expectedContent) }
        verify(exactly = 1) { settingsSavePort.saveLastSentAt(now) }
    }

    @Test
    fun `재시도도 실패하면 FAILED이고 마지막 전송 시각을 저장하지 않는다`() {
        due()
        every { reportSendPort.send(expectedContent) } throws IllegalStateException("디스코드 오류")

        service.sendIfDue(now) shouldBe ReportSendOutcome.FAILED

        verify(exactly = 2) { reportSendPort.send(expectedContent) }
        verify(exactly = 0) { settingsSavePort.saveLastSentAt(any()) }
    }

    @Test
    fun `지금 리포트는 전송 여부와 상관없이 현재 모드의 기간으로 만들고 설정을 바꾸지 않는다`() {
        every { settingsQueryPort.find() } returns
            ReportSettings(adMode = false, intervalDays = 3, lastSentAt = now.minusMinutes(5))
        every { getSignupStatsComparisonUseCase.getComparison(period.from, period.to) } returns comparison

        service.buildNow(now) shouldBe expectedContent

        verify(exactly = 0) { reportSendPort.send(any()) }
        verify(exactly = 0) { settingsSavePort.saveLastSentAt(any()) }
    }

    @Test
    fun `직전 기간을 비교하지 못하면 증감률 없이 리포트를 만든다`() {
        every { settingsQueryPort.find() } returns
            ReportSettings(adMode = false, intervalDays = 3, lastSentAt = null)
        every { getSignupStatsComparisonUseCase.getComparison(period.from, period.to) } returns
            SignupStatsComparison(stats, null)

        service.buildNow(now) shouldBe ReportContent(period, stats, null)
    }
}
