package parfait.core.stats.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.stats.domain.SignupGrowth
import parfait.core.stats.domain.SignupStats
import parfait.core.stats.domain.SignupStatsComparison
import parfait.core.stats.exception.StatsErrorCode
import parfait.core.stats.port.out.SignupStatsQueryPort
import java.time.LocalDateTime

class StatsServiceTest {
    private val queryPort = mockk<SignupStatsQueryPort>()
    private val service = StatsService(queryPort)

    private val from = LocalDateTime.of(2026, 10, 27, 0, 0)
    private val to = LocalDateTime.of(2026, 10, 30, 0, 0)

    @Test
    fun `기간을 포트에 넘기고 결과를 그대로 돌려준다`() {
        val stats = SignupStats(newSignups = 42, withdrawals = 3, newSignupWithdrawals = 1, totalUsers = 1284)
        every { queryPort.aggregate(from, to) } returns stats

        service.getSignupStats(from, to) shouldBe stats

        verify(exactly = 1) { queryPort.aggregate(from, to) }
    }

    @Test
    fun `순증은 신규 가입에서 탈퇴를 뺀 값이다`() {
        SignupStats(newSignups = 42, withdrawals = 3, newSignupWithdrawals = 1, totalUsers = 1284).netGrowth shouldBe 39
        SignupStats(newSignups = 1, withdrawals = 4, newSignupWithdrawals = 0, totalUsers = 10).netGrowth shouldBe -3
    }

    @Test
    fun `시작이 끝보다 늦거나 같으면 INVALID_STATS_PERIOD 예외가 난다`() {
        val sameTime = shouldThrow<BusinessException> { service.getSignupStats(from, from) }
        sameTime.errorCode shouldBe StatsErrorCode.INVALID_STATS_PERIOD

        val reversed = shouldThrow<BusinessException> { service.getSignupStats(to, from) }
        reversed.errorCode shouldBe StatsErrorCode.INVALID_STATS_PERIOD
    }

    @Test
    fun `정확히 366일 기간은 조회할 수 있다`() {
        val limitTo = from.plusDays(366)
        val stats = SignupStats(newSignups = 1, withdrawals = 0, newSignupWithdrawals = 0, totalUsers = 1)
        every { queryPort.aggregate(from, limitTo) } returns stats

        service.getSignupStats(from, limitTo) shouldBe stats

        verify(exactly = 1) { queryPort.aggregate(from, limitTo) }
    }

    @Test
    fun `366일을 1초라도 넘으면 INVALID_STATS_PERIOD 예외가 나고 포트를 호출하지 않는다`() {
        val tooLong = from.plusDays(366).plusSeconds(1)

        val ex = shouldThrow<BusinessException> { service.getSignupStats(from, tooLong) }

        ex.errorCode shouldBe StatsErrorCode.INVALID_STATS_PERIOD
        verify(exactly = 0) { queryPort.aggregate(any(), any()) }
    }

    @Test
    fun `비교 조회는 같은 길이의 직전 기간을 함께 조회한다`() {
        val previousFrom = from.minusDays(3)
        val current = SignupStats(newSignups = 42, withdrawals = 3, newSignupWithdrawals = 1, totalUsers = 1284)
        val previous = SignupStats(newSignups = 40, withdrawals = 2, newSignupWithdrawals = 0, totalUsers = 1245)
        every { queryPort.aggregate(from, to) } returns current
        every { queryPort.aggregate(previousFrom, from) } returns previous

        val comparison = service.getComparison(from, to)

        comparison shouldBe SignupStatsComparison(current, previous)
        comparison.growth shouldBe SignupGrowth.between(current, previous)
        verify(exactly = 1) { queryPort.aggregate(previousFrom, from) }
    }

    @Test
    fun `직전 기간 조회가 실패해도 현재 통계는 돌려주고 previous는 null이다`() {
        val current = SignupStats(newSignups = 42, withdrawals = 3, newSignupWithdrawals = 1, totalUsers = 1284)
        every { queryPort.aggregate(from, to) } returns current
        every { queryPort.aggregate(from.minusDays(3), from) } throws IllegalStateException("DB 오류")

        val comparison = service.getComparison(from, to)

        comparison.current shouldBe current
        comparison.previous shouldBe null
        comparison.growth shouldBe null
    }

    @Test
    fun `비교 조회도 현재 기간이 잘못되면 INVALID_STATS_PERIOD 예외가 난다`() {
        val ex = shouldThrow<BusinessException> { service.getComparison(to, from) }

        ex.errorCode shouldBe StatsErrorCode.INVALID_STATS_PERIOD
        verify(exactly = 0) { queryPort.aggregate(any(), any()) }
    }
}
