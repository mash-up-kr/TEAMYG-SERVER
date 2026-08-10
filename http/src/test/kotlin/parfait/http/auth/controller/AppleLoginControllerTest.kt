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
import parfait.core.auth.port.`in`.AppleLoginResult
import parfait.core.auth.port.`in`.AppleLoginUseCase
import parfait.core.exception.BusinessException
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [AppleLoginController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
    AppleLoginControllerTest.FakeAppleLoginUseCaseConfig::class,
)
class AppleLoginControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var fakeAppleLoginUseCase: FakeAppleLoginUseCase

    @BeforeEach
    fun resetFake() {
        fakeAppleLoginUseCase.result = null
        fakeAppleLoginUseCase.exception = null
    }

    @TestConfiguration
    class FakeAppleLoginUseCaseConfig {
        @Bean
        fun appleLoginUseCase(): FakeAppleLoginUseCase = FakeAppleLoginUseCase()
    }

    class FakeAppleLoginUseCase : AppleLoginUseCase {
        var result: AppleLoginResult? = null
        var exception: RuntimeException? = null

        override fun login(
            identityToken: String,
            nonce: String,
            authorizationCode: String,
        ): AppleLoginResult {
            exception?.let { throw it }
            return result ?: error("설정된 result가 없습니다")
        }
    }

    private fun requestBody(
        identityToken: String = "identity-token",
        nonce: String = "nonce",
        authorizationCode: String = "auth-code",
    ) = """{"identityToken":"$identityToken","nonce":"$nonce","authorizationCode":"$authorizationCode"}"""

    @Test
    fun `기존 회원이면 200과 액세스-리프레시 토큰을 응답한다`() {
        fakeAppleLoginUseCase.result = AppleLoginResult.ExistingMember("access-token", "refresh-token", 3600)

        mockMvc
            .post("/api/v1/auth/apple") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.isNewUser") { value(false) }
                jsonPath("$.data.accessToken") { value("access-token") }
                jsonPath("$.data.refreshToken") { value("refresh-token") }
                jsonPath("$.data.expiresIn") { value(3600) }
                jsonPath("$.data.registrationToken") { doesNotExist() }
            }
    }

    @Test
    fun `신규 유저면 200과 registrationToken을 응답한다`() {
        fakeAppleLoginUseCase.result = AppleLoginResult.NewUser("registration-token")

        mockMvc
            .post("/api/v1/auth/apple") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.isNewUser") { value(true) }
                jsonPath("$.data.registrationToken") { value("registration-token") }
                jsonPath("$.data.accessToken") { doesNotExist() }
            }
    }

    @Test
    fun `identityToken이 비어 있으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/api/v1/auth/apple") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody(identityToken = "")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }

    @Test
    fun `authorizationCode가 비어 있으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/api/v1/auth/apple") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody(authorizationCode = "")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }

    @Test
    fun `ID 토큰 검증에 실패하면 401과 INVALID_ID_TOKEN으로 응답한다`() {
        fakeAppleLoginUseCase.exception = BusinessException(AuthErrorCode.INVALID_ID_TOKEN)

        mockMvc
            .post("/api/v1/auth/apple") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_ID_TOKEN") }
            }
    }

    @Test
    fun `애플 서버 응답 오류면 502와 APPLE_SERVER_ERROR로 응답한다`() {
        fakeAppleLoginUseCase.exception = BusinessException(AuthErrorCode.APPLE_SERVER_ERROR)

        mockMvc
            .post("/api/v1/auth/apple") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isBadGateway() }
                jsonPath("$.code") { value("APPLE_SERVER_ERROR") }
            }
    }

    @Test
    fun `애플 서버에 연결할 수 없으면 503과 APPLE_SERVER_UNAVAILABLE으로 응답한다`() {
        fakeAppleLoginUseCase.exception = BusinessException(AuthErrorCode.APPLE_SERVER_UNAVAILABLE)

        mockMvc
            .post("/api/v1/auth/apple") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isServiceUnavailable() }
                jsonPath("$.code") { value("APPLE_SERVER_UNAVAILABLE") }
            }
    }
}
