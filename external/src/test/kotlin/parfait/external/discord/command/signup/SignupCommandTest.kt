package parfait.external.discord.command.signup

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.stats.domain.DailySignupStats
import parfait.core.stats.domain.SignupStats
import parfait.core.stats.domain.SignupStatsComparison
import parfait.core.stats.port.`in`.GetDailySignupStatsUseCase
import parfait.core.stats.port.`in`.GetGroupStatsUseCase
import parfait.core.stats.port.`in`.GetSignupStatsComparisonUseCase
import parfait.external.discord.command.CommandRequest
import parfait.external.discord.command.fixedClock
import parfait.external.discord.report.ReportEmbedRenderer
import java.time.LocalDate
import java.time.LocalDateTime

class SignupCommandTest {
    private val comparisonUseCase = mockk<GetSignupStatsComparisonUseCase>()
    private val dailyUseCase = mockk<GetDailySignupStatsUseCase>()
    private val groupUseCase = mockk<GetGroupStatsUseCase>()
    private val now = LocalDateTime.of(2026, 10, 30, 21, 0)
    private val command =
        SignupCommand(comparisonUseCase, dailyUseCase, groupUseCase, ReportEmbedRenderer(), fixedClock(now))
    private val stats = SignupStats(newSignups = 42, withdrawals = 3, newSignupWithdrawals = 1, totalUsers = 1284)

    private val previous = SignupStats(newSignups = 40, withdrawals = 2, newSignupWithdrawals = 0, totalUsers = 1245)

    private fun request(options: Map<String, String>) =
        CommandRequest(subcommand = null, options = options, isAdmin = false)

    /** 오늘을 포함한 달력 N일의 시작 시각(오늘 − (N−1)일 0시). */
    private fun startOf(days: Long) = now.toLocalDate().minusDays(days - 1).atStartOfDay()

    private fun stub(
        days: Long,
        daily: List<DailySignupStats> = emptyList(),
    ) {
        val from = startOf(days)
        every { comparisonUseCase.getComparison(from, now) } returns SignupStatsComparison(stats, previous)
        every { dailyUseCase.getDailySignupStats(from, now) } returns daily
        every { groupUseCase.countNewGroups(from, now) } returns 4
    }

    @Test
    fun `선택한 일수만큼 오늘을 포함한 달력 구간을 채널에 공개로 보여준다`() {
        stub(30)

        val reply = command.execute(request(mapOf("기간" to "30")))

        reply.ephemeral shouldBe false
        reply.embed!!.title shouldBe "📊 가입자 리포트 (10/1 00:00 ~ 10/30 21:00 기준)"
        verify(exactly = 1) { comparisonUseCase.getComparison(startOf(30), now) }
    }

    @Test
    fun `요약에 직전 기간 대비 증감률과 신규 그룹 수가 들어간다`() {
        stub(7)

        val value =
            command
                .execute(request(emptyMap()))
                .embed!!
                .fields
                .first()
                .value

        value shouldBe
            "신규 가입 42명 (▲5%) · 탈퇴 3명 (신규 중 1명) · 순증 +39명\n" +
            "누적 가입자 1,284명 (+3.1%)\n" +
            "신규 그룹 4개"
    }

    @Test
    fun `요약 아래에 달력 날짜별 일별 상세가 붙는다`() {
        stub(
            3,
            listOf(
                DailySignupStats(LocalDate.of(2026, 10, 28), newSignups = 3, withdrawals = 1),
                DailySignupStats(LocalDate.of(2026, 10, 29), newSignups = 5, withdrawals = 0),
                DailySignupStats(LocalDate.of(2026, 10, 30), newSignups = 2, withdrawals = 1),
            ),
        )

        val fields = command.execute(request(mapOf("기간" to "3"))).embed!!.fields

        fields.map { it.name } shouldBe listOf("서비스", "일별 10/28~10/30")
        fields[1].value shouldBe
            "10/28 (수) : 가입 +3명 / 탈퇴 -1명\n" +
            "10/29 (목) : 가입 +5명 / 탈퇴 -0명\n" +
            "10/30 (금) : 가입 +2명 / 탈퇴 -1명 (진행 중)"
    }

    @Test
    fun `기간을 고르지 않으면 7일이다`() {
        stub(7)

        command.execute(request(emptyMap())).embed!!.title shouldBe "📊 가입자 리포트 (10/24 00:00 ~ 10/30 21:00 기준)"
    }

    @Test
    fun `지원하지 않는 기간이면 본인에게만 안내하고 조회하지 않는다`() {
        val reply = command.execute(request(mapOf("기간" to "5")))

        reply.text shouldBe "지원하지 않는 기간입니다. 1일·3일·7일·30일 중에서 골라 주세요."
        reply.ephemeral shouldBe true
        verify(exactly = 0) { comparisonUseCase.getComparison(any(), any()) }
        verify(exactly = 0) { dailyUseCase.getDailySignupStats(any(), any()) }
        verify(exactly = 0) { groupUseCase.countNewGroups(any(), any()) }
    }

    @Test
    fun `자정 정각에 1일을 조회해도 최소 1초 구간으로 조회한다`() {
        val midnight = LocalDateTime.of(2026, 11, 7, 0, 0, 0)
        val midnightCommand =
            SignupCommand(comparisonUseCase, dailyUseCase, groupUseCase, ReportEmbedRenderer(), fixedClock(midnight))
        val to = midnight.plusSeconds(1)
        every { comparisonUseCase.getComparison(midnight, to) } returns SignupStatsComparison(stats, previous)
        every { dailyUseCase.getDailySignupStats(midnight, to) } returns emptyList()
        every { groupUseCase.countNewGroups(midnight, to) } returns 0

        val reply = midnightCommand.execute(request(mapOf("기간" to "1")))

        reply.ephemeral shouldBe false
        verify(exactly = 1) { comparisonUseCase.getComparison(midnight, to) }
        verify(exactly = 1) { dailyUseCase.getDailySignupStats(midnight, to) }
        verify(exactly = 1) { groupUseCase.countNewGroups(midnight, to) }
    }
}
