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
import parfait.core.auth.port.`in`.KakaoLoginResult
import parfait.core.auth.port.`in`.KakaoLoginUseCase
import parfait.core.exception.BusinessException
import parfait.http.exception.GlobalExceptionHandler
import parfait.http.security.TestMemberQueryPortConfig
import parfait.http.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [KakaoLoginController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
    KakaoLoginControllerTest.FakeKakaoLoginUseCaseConfig::class,
)
class KakaoLoginControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var fakeKakaoLoginUseCase: FakeKakaoLoginUseCase

    @BeforeEach
    fun resetFake() {
        fakeKakaoLoginUseCase.result = null
        fakeKakaoLoginUseCase.exception = null
    }

    @TestConfiguration
    class FakeKakaoLoginUseCaseConfig {
        @Bean
        fun kakaoLoginUseCase(): FakeKakaoLoginUseCase = FakeKakaoLoginUseCase()
    }

    class FakeKakaoLoginUseCase : KakaoLoginUseCase {
        var result: KakaoLoginResult? = null
        var exception: RuntimeException? = null

        override fun login(
            idToken: String,
            nonce: String,
        ): KakaoLoginResult {
            exception?.let { throw it }
            return result ?: error("설정된 result가 없습니다")
        }
    }

    private fun requestBody(
        idToken: String = "id-token",
        nonce: String = "nonce",
    ) = """{"idToken":"$idToken","nonce":"$nonce"}"""

    @Test
    fun `기존 회원이면 200과 액세스-리프레시 토큰을 응답한다`() {
        fakeKakaoLoginUseCase.result = KakaoLoginResult.ExistingMember("access-token", "refresh-token", 3600)

        mockMvc
            .post("/api/v1/auth/kakao") {
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
        fakeKakaoLoginUseCase.result = KakaoLoginResult.NewUser("registration-token")

        mockMvc
            .post("/api/v1/auth/kakao") {
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
    fun `idToken이 비어 있으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/api/v1/auth/kakao") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody(idToken = "")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }

    @Test
    fun `ID 토큰 검증에 실패하면 401과 INVALID_ID_TOKEN으로 응답한다`() {
        fakeKakaoLoginUseCase.exception = BusinessException(AuthErrorCode.INVALID_ID_TOKEN)

        mockMvc
            .post("/api/v1/auth/kakao") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_ID_TOKEN") }
            }
    }

    @Test
    fun `카카오 JWKS 조회에 실패하면 502와 KAKAO_JWKS_FETCH_FAILED로 응답한다`() {
        fakeKakaoLoginUseCase.exception = BusinessException(AuthErrorCode.KAKAO_JWKS_FETCH_FAILED)

        mockMvc
            .post("/api/v1/auth/kakao") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isBadGateway() }
                jsonPath("$.code") { value("KAKAO_JWKS_FETCH_FAILED") }
            }
    }

    @Test
    fun `카카오 서버에 연결할 수 없으면 503과 KAKAO_SERVER_UNAVAILABLE으로 응답한다`() {
        fakeKakaoLoginUseCase.exception = BusinessException(AuthErrorCode.KAKAO_SERVER_UNAVAILABLE)

        mockMvc
            .post("/api/v1/auth/kakao") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isServiceUnavailable() }
                jsonPath("$.code") { value("KAKAO_SERVER_UNAVAILABLE") }
            }
    }
}
