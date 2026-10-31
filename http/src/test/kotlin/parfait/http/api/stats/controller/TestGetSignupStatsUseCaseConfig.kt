package parfait.http.api.stats.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.stats.domain.SignupStats
import parfait.core.stats.port.`in`.GetSignupStatsUseCase
import java.time.LocalDateTime

/**
 * `GetSignupStatsUseCase` 의 실제 구현체(`StatsService`)는 `core` 에 있고 `http` 의 `TestApplication` 은
 * `parfait.http` 만 스캔하므로, 컨텍스트 로딩만 필요한 테스트를 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestGetSignupStatsUseCaseConfig {
    @Bean
    fun getSignupStatsUseCase(): GetSignupStatsUseCase =
        object : GetSignupStatsUseCase {
            override fun getSignupStats(
                from: LocalDateTime,
                to: LocalDateTime,
            ) = SignupStats(newSignups = 0, withdrawals = 0, newSignupWithdrawals = 0, totalUsers = 0)
        }
}
