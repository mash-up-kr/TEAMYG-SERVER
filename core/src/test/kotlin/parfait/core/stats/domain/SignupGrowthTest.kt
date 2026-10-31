package parfait.core.stats.domain

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SignupGrowthTest {
    private fun stats(
        newSignups: Long,
        totalUsers: Long,
    ) = SignupStats(newSignups = newSignups, withdrawals = 0, newSignupWithdrawals = 0, totalUsers = totalUsers)

    @Test
    fun `신규 가입이 늘면 양수 증감률이다`() {
        SignupGrowth.between(stats(115, 1000), stats(100, 1000)).newSignupsChangePercent shouldBe 15.0
    }

    @Test
    fun `신규 가입이 줄면 음수 증감률이다`() {
        SignupGrowth.between(stats(80, 1000), stats(100, 1000)).newSignupsChangePercent shouldBe -20.0
    }

    @Test
    fun `신규 가입이 같으면 증감률이 0이다`() {
        SignupGrowth.between(stats(100, 1000), stats(100, 1000)).newSignupsChangePercent shouldBe 0.0
    }

    @Test
    fun `직전 신규 가입이 0이면 신규 증감률은 null이다`() {
        SignupGrowth.between(stats(10, 1000), stats(0, 1000)).newSignupsChangePercent shouldBe null
    }

    @Test
    fun `직전 누적이 0이면 누적 증가율은 null이다`() {
        SignupGrowth.between(stats(10, 1000), stats(5, 0)).totalUsersGrowthPercent shouldBe null
    }

    @Test
    fun `누적 증가율은 소수까지 계산한다`() {
        SignupGrowth
            .between(stats(10, 1284), stats(10, 1245))
            .totalUsersGrowthPercent!!
            .shouldBe(3.1325.plusOrMinus(0.001))
    }

    @Test
    fun `누적이 줄면 음수 증가율이다`() {
        SignupGrowth.between(stats(10, 900), stats(10, 1000)).totalUsersGrowthPercent shouldBe -10.0
    }
}
