package parfait.http.auth.controller

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
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.`in`.LogoutUseCase
import parfait.core.exception.BusinessException
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

/**
 * `authentication`이 없을 때의 401 응답은 이 슬라이스가 아니라
 * [parfait.http.global.security.SecurityConfigIntegrationTest]에서 실제 `SecurityConfig` +
 * `JwtAuthFilter`로 검증한다. 이 테스트는 `@AutoConfigureMockMvc(addFilters = false)`라
 * 실제 보안 필터가 동작하지 않아 인증 부재 시나리오를 재현할 수 없다
 * ([parfait.http.parfaitgroup.ParfaitGroupControllerTest]와 동일한 패턴).
 */
@WebMvcTest(controllers = [LogoutController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
    LogoutControllerTest.LogoutUseCaseConfig::class,
)
class LogoutControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var logoutUseCase: LogoutUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @BeforeEach
    fun resetMock() {
        clearMocks(logoutUseCase)
    }

    @TestConfiguration
    class LogoutUseCaseConfig {
        @Bean
        fun logoutUseCase(): LogoutUseCase = mockk(relaxed = true)
    }

    @Test
    fun `인증된 요청이면 refreshToken을 넘겨 로그아웃하고 204를 응답한다`() {
        mockMvc
            .post("/api/v1/auth/logout") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"refreshToken":"refresh-token"}"""
            }.andExpect {
                status { isNoContent() }
            }

        verify { logoutUseCase.logout(42L, "refresh-token") }
    }

    @Test
    fun `refreshToken이 비어 있으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/api/v1/auth/logout") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"refreshToken":""}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }

        verify(exactly = 0) { logoutUseCase.logout(any(), any()) }
    }

    @Test
    fun `body가 없으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/api/v1/auth/logout") {
                principal = authentication
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }

        verify(exactly = 0) { logoutUseCase.logout(any(), any()) }
    }

    @Test
    fun `다른 회원의 refreshToken이면 403과 FORBIDDEN_REFRESH_TOKEN으로 응답한다`() {
        every { logoutUseCase.logout(42L, "other-token") } throws
            BusinessException(AuthErrorCode.FORBIDDEN_REFRESH_TOKEN)

        mockMvc
            .post("/api/v1/auth/logout") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"refreshToken":"other-token"}"""
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("FORBIDDEN_REFRESH_TOKEN") }
            }
    }
}
