package parfait.http.api.stats.controller

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.core.exception.BusinessException
import parfait.core.stats.domain.SignupStats
import parfait.core.stats.exception.StatsErrorCode
import parfait.core.stats.port.`in`.GetSignupStatsUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import java.time.LocalDateTime

@WebMvcTest(controllers = [StatsController::class], properties = ["stats.api-token=test-stats-token"])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    StatsTokenVerifier::class,
    StatsControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class StatsControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var getSignupStatsUseCase: GetSignupStatsUseCase

    @BeforeEach
    fun resetMocks() {
        clearMocks(getSignupStatsUseCase)
    }

    @Test
    fun `올바른 토큰이면 기간의 가입자 통계를 돌려준다`() {
        every {
            getSignupStatsUseCase.getSignupStats(
                LocalDateTime.of(2026, 10, 27, 0, 0),
                LocalDateTime.of(2026, 10, 30, 0, 0),
            )
        } returns SignupStats(newSignups = 42, withdrawals = 3, newSignupWithdrawals = 1, totalUsers = 1284)

        mockMvc
            .get("/admin/stats?from=2026-10-27&to=2026-10-30") {
                header("X-Stats-Token", "test-stats-token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.newSignups") { value(42) }
                jsonPath("$.data.withdrawals") { value(3) }
                jsonPath("$.data.newSignupWithdrawals") { value(1) }
                jsonPath("$.data.netGrowth") { value(39) }
                jsonPath("$.data.totalUsers") { value(1284) }
            }
    }

    @Test
    fun `토큰이 없으면 401이고 통계를 조회하지 않는다`() {
        mockMvc.get("/admin/stats?from=2026-10-27&to=2026-10-30").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("INVALID_STATS_TOKEN") }
        }

        verify(exactly = 0) { getSignupStatsUseCase.getSignupStats(any(), any()) }
    }

    @Test
    fun `토큰이 틀리면 401이다`() {
        mockMvc
            .get("/admin/stats?from=2026-10-27&to=2026-10-30") {
                header("X-Stats-Token", "wrong-token")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_STATS_TOKEN") }
            }

        verify(exactly = 0) { getSignupStatsUseCase.getSignupStats(any(), any()) }
    }

    @Test
    fun `기간이 잘못되면 UseCase 의 예외가 400으로 변환된다`() {
        every { getSignupStatsUseCase.getSignupStats(any(), any()) } throws
            BusinessException(StatsErrorCode.INVALID_STATS_PERIOD)

        mockMvc
            .get("/admin/stats?from=2026-10-30&to=2026-10-27") {
                header("X-Stats-Token", "test-stats-token")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_STATS_PERIOD") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun getSignupStatsUseCase(): GetSignupStatsUseCase = mockk()
    }
}
