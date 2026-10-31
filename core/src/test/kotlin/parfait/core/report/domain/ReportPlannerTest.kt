package parfait.core.report.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReportPlannerTest {
    private val planner = ReportPlanner()

    // 10/30 21:00 에 스케줄러가 돈다.
    private val now = LocalDateTime.of(2026, 10, 30, 21, 0, 0)

    private fun settings(
        adMode: Boolean = false,
        lastSentAt: LocalDateTime? = null,
    ) = ReportSettings(adMode = adMode, intervalDays = 3, lastSentAt = lastSentAt)

    @Test
    fun `평소 모드에서 처음 실행하면 최근 3일을 보낸다`() {
        planner.plan(settings(), now) shouldBe ReportPeriod(now.minusDays(3), now)
    }

    @Test
    fun `평소 모드에서 마지막 전송이 3일 전이면 마지막 전송부터 지금까지를 보낸다`() {
        // 전송이 21:00:03 에 끝났어도 날짜 차이로 비교하므로 3일 뒤 21:00:00 에 대상이 된다.
        val last = LocalDateTime.of(2026, 10, 27, 21, 0, 3)

        planner.plan(settings(lastSentAt = last), now) shouldBe ReportPeriod(last, now)
    }

    @Test
    fun `평소 모드에서 마지막 전송이 2일 전이면 건너뛴다`() {
        val last = LocalDateTime.of(2026, 10, 28, 21, 0, 3)

        planner.plan(settings(lastSentAt = last), now) shouldBe null
    }

    @Test
    fun `오늘 이미 전송했으면 건너뛴다`() {
        val last = LocalDateTime.of(2026, 10, 30, 9, 0, 0)

        planner.plan(settings(lastSentAt = last), now) shouldBe null
        planner.plan(settings(adMode = true, lastSentAt = last), now) shouldBe null
    }

    @Test
    fun `광고모드에서 처음 실행하면 직전 24시간을 보낸다`() {
        planner.plan(settings(adMode = true), now) shouldBe ReportPeriod(now.minusDays(1), now)
    }

    @Test
    fun `광고모드에서는 마지막 전송이 어제여도 대상이다`() {
        val last = LocalDateTime.of(2026, 10, 29, 21, 0, 3)

        planner.plan(settings(adMode = true, lastSentAt = last), now) shouldBe ReportPeriod(last, now)
    }

    private val sendTime = LocalTime.of(21, 0)
    private val today = LocalDate.of(2026, 10, 30)
    private val beforeSend = LocalDateTime.of(2026, 10, 30, 20, 0, 0)
    private val afterSend = LocalDateTime.of(2026, 10, 30, 22, 0, 0)

    private fun nextDate(
        settings: ReportSettings,
        at: LocalDateTime,
    ) = planner.nextReportDate(settings, at, sendTime)

    @Test
    fun `다음 리포트 날짜 - 처음이면 오늘이다`() {
        nextDate(settings(), beforeSend) shouldBe today
        nextDate(settings(adMode = true), beforeSend) shouldBe today
    }

    @Test
    fun `다음 리포트 날짜 - 처음이어도 전송 시각이 지났으면 내일이다`() {
        nextDate(settings(), afterSend) shouldBe today.plusDays(1)
        nextDate(settings(adMode = true), afterSend) shouldBe today.plusDays(1)
    }

    @Test
    fun `다음 리포트 날짜 - 전송 시각 경계는 20시59분59초까지 오늘, 정각부터 내일이다`() {
        nextDate(settings(), LocalDateTime.of(2026, 10, 30, 20, 59, 59)) shouldBe today
        nextDate(settings(), LocalDateTime.of(2026, 10, 30, 21, 0, 0)) shouldBe today.plusDays(1)
    }

    @Test
    fun `다음 리포트 날짜 - 평소 모드는 마지막 전송일에 주기를 더한 날이다`() {
        val last = LocalDateTime.of(2026, 10, 28, 21, 0, 3)

        nextDate(settings(lastSentAt = last), beforeSend) shouldBe LocalDate.of(2026, 10, 31)
    }

    @Test
    fun `다음 리포트 날짜 - 주기가 지났으면 오늘이다`() {
        val last = LocalDateTime.of(2026, 10, 20, 21, 0, 3)

        nextDate(settings(lastSentAt = last), beforeSend) shouldBe today
    }

    @Test
    fun `다음 리포트 날짜 - 주기가 지났어도 전송 시각 이후면 내일이다`() {
        val last = LocalDateTime.of(2026, 10, 20, 21, 0, 3)

        nextDate(settings(lastSentAt = last), afterSend) shouldBe today.plusDays(1)
    }

    @Test
    fun `다음 리포트 날짜 - 광고모드는 오늘 보냈으면 내일, 아니면 오늘이다`() {
        val sentToday = LocalDateTime.of(2026, 10, 30, 9, 0, 0)
        val sentYesterday = LocalDateTime.of(2026, 10, 29, 21, 0, 3)

        nextDate(settings(adMode = true, lastSentAt = sentToday), beforeSend) shouldBe LocalDate.of(2026, 10, 31)
        nextDate(settings(adMode = true, lastSentAt = sentYesterday), beforeSend) shouldBe today
    }

    @Test
    fun `다음 리포트 날짜 - 평소 모드로 건너뛰는 날 21시 이후에 광고모드를 켜면 내일이다`() {
        val last = LocalDateTime.of(2026, 10, 29, 21, 0, 3)

        nextDate(settings(adMode = true, lastSentAt = last), afterSend) shouldBe LocalDate.of(2026, 10, 31)
    }

    @Test
    fun `다음 리포트 날짜 - 평소 모드에서 오늘 21시 이후 전송했으면 마지막 전송일에 주기를 더한 날이다`() {
        val last = LocalDateTime.of(2026, 10, 30, 21, 0, 3)

        nextDate(settings(lastSentAt = last), afterSend) shouldBe LocalDate.of(2026, 11, 2)
    }

    @Test
    fun `다음 리포트 날짜 - 연말 경계를 넘어 계산한다`() {
        val last = LocalDateTime.of(2026, 12, 31, 21, 0, 3)
        val at = LocalDateTime.of(2027, 1, 1, 10, 0, 0)

        nextDate(settings(lastSentAt = last), at) shouldBe LocalDate.of(2027, 1, 3)
    }

    @Test
    fun `다음 리포트 날짜 - 월말 경계를 넘어 계산한다`() {
        val last = LocalDateTime.of(2026, 10, 30, 21, 0, 3)
        val at = LocalDateTime.of(2026, 10, 31, 10, 0, 0)

        nextDate(settings(lastSentAt = last), at) shouldBe LocalDate.of(2026, 11, 2)
    }

    @Test
    fun `지금 리포트 기간 - 광고모드는 직전 24시간, 평소 모드는 직전 주기 일수다`() {
        planner.planNow(settings(adMode = true), now) shouldBe ReportPeriod(now.minusDays(1), now)
        planner.planNow(settings(), now) shouldBe ReportPeriod(now.minusDays(3), now)
    }

    @Test
    fun `지금 리포트 기간 - 마지막 전송 시각은 무시한다`() {
        val last = LocalDateTime.of(2026, 10, 29, 21, 0, 3)

        planner.planNow(settings(lastSentAt = last), now) shouldBe ReportPeriod(now.minusDays(3), now)
        planner.planNow(settings(adMode = true, lastSentAt = last), now) shouldBe ReportPeriod(now.minusDays(1), now)
    }
}
