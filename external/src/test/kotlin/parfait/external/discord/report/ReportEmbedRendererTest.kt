package parfait.external.discord.report

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import parfait.core.report.domain.ReportContent
import parfait.core.report.domain.ReportPeriod
import parfait.core.stats.domain.DailySignupStats
import parfait.core.stats.domain.SignupGrowth
import parfait.core.stats.domain.SignupStats
import java.time.LocalDate
import java.time.LocalDateTime

class ReportEmbedRendererTest {
    private val renderer = ReportEmbedRenderer()
    private val period = ReportPeriod(LocalDateTime.of(2026, 10, 27, 21, 0), LocalDateTime.of(2026, 10, 30, 21, 0))

    @Test
    fun `제목에 기준 기간이 들어간다`() {
        val embed = renderer.render(ReportContent(period, SignupStats(42, 3, 1, 1284)))

        embed.title shouldBe "📊 가입자 리포트 (10/27 21:00 ~ 10/30 21:00 기준)"
    }

    @Test
    fun `서비스 필드에 신규 탈퇴 신규 중 탈퇴 순증 누적이 들어간다`() {
        val embed = renderer.render(ReportContent(period, SignupStats(42, 3, 1, 1284)))

        val field = embed.fields.single()
        field.name shouldBe "서비스"
        field.value shouldBe "신규 가입 42명 · 탈퇴 3명 (신규 중 1명) · 순증 +39명\n누적 가입자 1,284명"
    }

    @Test
    fun `순증이 음수면 부호가 마이너스이고 0이면 부호가 없다`() {
        val negative = renderer.render(ReportContent(period, SignupStats(1, 4, 0, 10)))
        negative.fields.single().value shouldBe "신규 가입 1명 · 탈퇴 4명 (신규 중 0명) · 순증 -3명\n누적 가입자 10명"

        val zero = renderer.render(ReportContent(period, SignupStats(2, 2, 0, 10)))
        zero.fields.single().value shouldBe "신규 가입 2명 · 탈퇴 2명 (신규 중 0명) · 순증 0명\n누적 가입자 10명"
    }

    private fun value(growth: SignupGrowth?) =
        renderer
            .render(ReportContent(period, SignupStats(42, 3, 1, 1284), growth))
            .fields
            .single()
            .value!!

    @Test
    fun `신규 가입이 늘면 위 화살표와 정수 퍼센트가 붙는다`() {
        value(SignupGrowth(15.0, null)) shouldBe
            "신규 가입 42명 (▲15%) · 탈퇴 3명 (신규 중 1명) · 순증 +39명\n누적 가입자 1,284명"
    }

    @Test
    fun `신규 가입이 줄면 아래 화살표가 붙고 퍼센트는 반올림한다`() {
        value(SignupGrowth(-19.6, null)) shouldBe
            "신규 가입 42명 (▼20%) · 탈퇴 3명 (신규 중 1명) · 순증 +39명\n누적 가입자 1,284명"
    }

    @Test
    fun `신규 가입이 변하지 않으면 플러스마이너스 0퍼센트다`() {
        value(SignupGrowth(0.0, null)) shouldBe
            "신규 가입 42명 (±0%) · 탈퇴 3명 (신규 중 1명) · 순증 +39명\n누적 가입자 1,284명"
    }

    @Test
    fun `직전 신규 가입이 0명이면 직전 0명이라고 표시한다`() {
        value(SignupGrowth(null, null)) shouldBe
            "신규 가입 42명 (직전 0명) · 탈퇴 3명 (신규 중 1명) · 순증 +39명\n누적 가입자 1,284명"
    }

    @Test
    fun `누적 증가율은 소수점 한 자리이고 양수만 플러스가 붙는다`() {
        value(SignupGrowth(15.0, 3.1325)) shouldBe
            "신규 가입 42명 (▲15%) · 탈퇴 3명 (신규 중 1명) · 순증 +39명\n누적 가입자 1,284명 (+3.1%)"
        value(SignupGrowth(15.0, -0.4)).endsWith("누적 가입자 1,284명 (-0.4%)") shouldBe true
        value(SignupGrowth(15.0, 0.0)).endsWith("누적 가입자 1,284명 (0.0%)") shouldBe true
    }

    private val dailyPeriod =
        ReportPeriod(LocalDateTime.of(2026, 11, 3, 0, 0), LocalDateTime.of(2026, 11, 5, 21, 30))
    private val dailyStats = SignupStats(10, 2, 0, 100)

    @Test
    fun `신규 그룹 수가 있으면 서비스 필드 마지막 줄에 표시한다`() {
        val embed = renderer.render(ReportContent(period, SignupStats(42, 3, 1, 1284), newGroups = 5))

        embed.fields.single().value shouldBe
            "신규 가입 42명 · 탈퇴 3명 (신규 중 1명) · 순증 +39명\n누적 가입자 1,284명\n신규 그룹 5개"
    }

    @Test
    fun `일별 상세가 있으면 일별 필드를 추가하고 오늘 줄에 진행 중을 붙인다`() {
        val daily =
            listOf(
                DailySignupStats(LocalDate.of(2026, 11, 3), newSignups = 3, withdrawals = 1),
                DailySignupStats(LocalDate.of(2026, 11, 4), newSignups = 5, withdrawals = 0),
                DailySignupStats(LocalDate.of(2026, 11, 5), newSignups = 2, withdrawals = 1),
            )

        val embed = renderer.render(ReportContent(dailyPeriod, dailyStats, daily = daily))

        embed.fields.map { it.name } shouldBe listOf("서비스", "일별 11/03~11/05")
        embed.fields[1].value shouldBe
            "11/03 (화) : 가입 +3명 / 탈퇴 -1명\n" +
            "11/04 (수) : 가입 +5명 / 탈퇴 -0명\n" +
            "11/05 (목) : 가입 +2명 / 탈퇴 -1명 (진행 중)"
    }

    @Test
    fun `구간 끝이 자정이면 진행 중을 붙이지 않는다`() {
        val closedPeriod =
            ReportPeriod(LocalDateTime.of(2026, 11, 3, 0, 0), LocalDateTime.of(2026, 11, 5, 0, 0))
        val daily =
            listOf(
                DailySignupStats(LocalDate.of(2026, 11, 3), newSignups = 3, withdrawals = 1),
                DailySignupStats(LocalDate.of(2026, 11, 4), newSignups = 5, withdrawals = 0),
            )

        val embed = renderer.render(ReportContent(closedPeriod, dailyStats, daily = daily))

        embed.fields[1].value!!.contains("진행 중") shouldBe false
    }

    @Test
    fun `일별 상세는 7일 단위로 나눠 필드에 담는다`() {
        val firstDay = LocalDate.of(2026, 11, 1)
        val daily = (0L until 8L).map { DailySignupStats(firstDay.plusDays(it), newSignups = 1, withdrawals = 0) }
        val longPeriod =
            ReportPeriod(LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 9, 0, 0))

        val embed = renderer.render(ReportContent(longPeriod, dailyStats, daily = daily))

        embed.fields.map { it.name } shouldBe listOf("서비스", "일별 11/01~11/07", "일별 11/08~11/08")
        embed.fields[1]
            .value!!
            .lines()
            .size shouldBe 7
        embed.fields[2]
            .value!!
            .lines()
            .size shouldBe 1
    }
}
