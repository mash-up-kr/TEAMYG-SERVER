package parfait.external.discord.command.signup

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.stats.domain.SignupStats
import parfait.core.stats.domain.SignupStatsComparison
import parfait.core.stats.port.`in`.GetSignupStatsComparisonUseCase
import parfait.external.discord.command.CommandRequest
import parfait.external.discord.command.fixedClock
import parfait.external.discord.report.ReportEmbedRenderer
import java.time.LocalDateTime

class SignupCommandTest {
    private val useCase = mockk<GetSignupStatsComparisonUseCase>()
    private val now = LocalDateTime.of(2026, 10, 30, 21, 0)
    private val command = SignupCommand(useCase, ReportEmbedRenderer(), fixedClock(now))
    private val stats = SignupStats(newSignups = 42, withdrawals = 3, newSignupWithdrawals = 1, totalUsers = 1284)

    private val previous = SignupStats(newSignups = 40, withdrawals = 2, newSignupWithdrawals = 0, totalUsers = 1245)

    private fun request(options: Map<String, String>) =
        CommandRequest(subcommand = null, options = options, isAdmin = false)

    @Test
    fun `선택한 일수만큼 직전 구간의 가입자 통계를 요청자에게만 보여준다`() {
        every { useCase.getComparison(now.minusDays(30), now) } returns SignupStatsComparison(stats, previous)

        val reply = command.execute(request(mapOf("기간" to "30")))

        reply.ephemeral shouldBe true
        reply.embed!!.title shouldBe "📊 가입자 리포트 (9/30 21:00 ~ 10/30 21:00 기준)"
        verify(exactly = 1) { useCase.getComparison(now.minusDays(30), now) }
    }

    @Test
    fun `임베드에 직전 기간 대비 증감률이 들어간다`() {
        every { useCase.getComparison(now.minusDays(7), now) } returns SignupStatsComparison(stats, previous)

        val value =
            command
                .execute(request(emptyMap()))
                .embed!!
                .fields
                .single()
                .value

        value shouldBe "신규 가입 42명 (▲5%) · 탈퇴 3명 (신규 중 1명) · 순증 +39명\n누적 가입자 1,284명 (+3.1%)"
    }

    @Test
    fun `기간을 고르지 않으면 7일이다`() {
        every { useCase.getComparison(now.minusDays(7), now) } returns SignupStatsComparison(stats, previous)

        command.execute(request(emptyMap())).embed!!.title shouldBe "📊 가입자 리포트 (10/23 21:00 ~ 10/30 21:00 기준)"
    }

    @Test
    fun `지원하지 않는 기간이면 안내하고 조회하지 않는다`() {
        val reply = command.execute(request(mapOf("기간" to "5")))

        reply.text shouldBe "지원하지 않는 기간입니다. 1일·3일·7일·30일 중에서 골라 주세요."
        reply.ephemeral shouldBe true
        verify(exactly = 0) { useCase.getComparison(any(), any()) }
    }
}
