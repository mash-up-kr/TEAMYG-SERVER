package parfait.external.discord.command.reportnow

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.report.domain.ReportContent
import parfait.core.report.domain.ReportPeriod
import parfait.core.report.port.`in`.BuildReportNowUseCase
import parfait.core.stats.domain.SignupStats
import parfait.external.discord.command.CommandRequest
import parfait.external.discord.command.fixedClock
import parfait.external.discord.report.ReportEmbedRenderer
import java.time.LocalDateTime

class ReportNowCommandTest {
    private val useCase = mockk<BuildReportNowUseCase>()
    private val now = LocalDateTime.of(2026, 10, 30, 21, 0)
    private val command = ReportNowCommand(useCase, ReportEmbedRenderer(), fixedClock(now))

    @Test
    fun `지금 리포트를 채널에 공개로 보여준다`() {
        val content =
            ReportContent(
                ReportPeriod(now.minusDays(3), now),
                SignupStats(newSignups = 42, withdrawals = 3, newSignupWithdrawals = 1, totalUsers = 1284),
            )
        every { useCase.buildNow(now) } returns content

        val reply = command.execute(CommandRequest(subcommand = "지금", options = emptyMap(), isAdmin = false))

        reply.ephemeral shouldBe false
        reply.embed!!.title shouldBe "📊 가입자 리포트 (10/27 21:00 ~ 10/30 21:00 기준)"
    }
}
