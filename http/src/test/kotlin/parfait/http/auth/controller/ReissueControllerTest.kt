package parfait.http.auth.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.`in`.ReissueResult
import parfait.core.auth.port.`in`.ReissueUseCase
import parfait.core.exception.BusinessException
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [ReissueController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
    ReissueControllerTest.FakeReissueUseCaseConfig::class,
)
class ReissueControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var fakeReissueUseCase: FakeReissueUseCase

    @BeforeEach
    fun resetFake() {
        fakeReissueUseCase.result = null
        fakeReissueUseCase.exception = null
    }

    @TestConfiguration
    class FakeReissueUseCaseConfig {
        @Bean
        fun reissueUseCase(): FakeReissueUseCase = FakeReissueUseCase()
    }

    class FakeReissueUseCase : ReissueUseCase {
        var result: ReissueResult? = null
        var exception: RuntimeException? = null

        override fun reissue(refreshToken: String): ReissueResult {
            exception?.let { throw it }
            return result ?: error("설정된 result가 없습니다")
        }
    }

    private fun requestBody(refreshToken: String = "refresh-token") = """{"refreshToken":"$refreshToken"}"""

    @Test
    fun `유효한 refreshToken이면 200과 새 액세스-리프레시 토큰을 응답한다`() {
        fakeReissueUseCase.result = ReissueResult("new-access-token", "new-refresh-token", 3600)

        mockMvc
            .post("/api/v1/auth/reissue") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.accessToken") { value("new-access-token") }
                jsonPath("$.data.refreshToken") { value("new-refresh-token") }
                jsonPath("$.data.expiresIn") { value(3600) }
            }
    }

    @Test
    fun `refreshToken이 비어 있으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/api/v1/auth/reissue") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody(refreshToken = "")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }

    @Test
    fun `저장값과 불일치하면 401과 INVALID_TOKEN으로 응답한다`() {
        fakeReissueUseCase.exception = BusinessException(AuthErrorCode.INVALID_TOKEN)

        mockMvc
            .post("/api/v1/auth/reissue") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_TOKEN") }
            }
    }

    @Test
    fun `만료된 refreshToken이면 401과 EXPIRED_TOKEN으로 응답한다`() {
        fakeReissueUseCase.exception = BusinessException(AuthErrorCode.EXPIRED_TOKEN)

        mockMvc
            .post("/api/v1/auth/reissue") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("EXPIRED_TOKEN") }
            }
    }
}
