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
import parfait.core.auth.port.`in`.SignupResult
import parfait.core.auth.port.`in`.SignupUseCase
import parfait.core.auth.port.`in`.TermsAgreement
import parfait.core.exception.BusinessException
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [SignupController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
    SignupControllerTest.FakeSignupUseCaseConfig::class,
)
class SignupControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var fakeSignupUseCase: FakeSignupUseCase

    @BeforeEach
    fun resetFake() {
        fakeSignupUseCase.result = null
        fakeSignupUseCase.exception = null
    }

    @TestConfiguration
    class FakeSignupUseCaseConfig {
        @Bean
        fun signupUseCase(): FakeSignupUseCase = FakeSignupUseCase()
    }

    class FakeSignupUseCase : SignupUseCase {
        var result: SignupResult? = null
        var exception: RuntimeException? = null

        override fun signup(
            registrationToken: String,
            agreements: List<TermsAgreement>,
        ): SignupResult {
            exception?.let { throw it }
            return result ?: error("설정된 result가 없습니다")
        }
    }

    private fun requestBody(
        registrationToken: String = "reg-token",
        agreements: String = """[{"termsId":1,"agreed":true}]""",
    ) = """{"registrationToken":"$registrationToken","agreements":$agreements}"""

    @Test
    fun `회원가입에 성공하면 201과 access-refresh 토큰을 응답한다`() {
        fakeSignupUseCase.result = SignupResult("access-token", "refresh-token", 3600)

        mockMvc
            .post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isCreated() }
                jsonPath("$.data.accessToken") { value("access-token") }
                jsonPath("$.data.refreshToken") { value("refresh-token") }
                jsonPath("$.data.expiresIn") { value(3600) }
            }
    }

    @Test
    fun `registrationToken이 비어 있으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody(registrationToken = "")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }

    @Test
    fun `agreements 필드가 없으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"registrationToken":"reg-token"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }

    @Test
    fun `termsId가 중복되면 400과 DUPLICATE_TERMS_ID로 응답한다`() {
        fakeSignupUseCase.exception = BusinessException(AuthErrorCode.DUPLICATE_TERMS_ID)

        mockMvc
            .post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("DUPLICATE_TERMS_ID") }
            }
    }

    @Test
    fun `존재하지 않는 termsId면 400과 TERMS_NOT_FOUND로 응답한다`() {
        fakeSignupUseCase.exception = BusinessException(AuthErrorCode.TERMS_NOT_FOUND)

        mockMvc
            .post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("TERMS_NOT_FOUND") }
            }
    }

    @Test
    fun `필수 약관 미동의면 400과 REQUIRED_TERMS_NOT_AGREED로 응답한다`() {
        fakeSignupUseCase.exception = BusinessException(AuthErrorCode.REQUIRED_TERMS_NOT_AGREED)

        mockMvc
            .post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("REQUIRED_TERMS_NOT_AGREED") }
            }
    }

    @Test
    fun `registrationToken이 만료되었으면 401과 EXPIRED_TOKEN으로 응답한다`() {
        fakeSignupUseCase.exception = BusinessException(AuthErrorCode.EXPIRED_TOKEN)

        mockMvc
            .post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("EXPIRED_TOKEN") }
            }
    }

    @Test
    fun `registrationToken이 위조되었으면 401과 INVALID_TOKEN으로 응답한다`() {
        fakeSignupUseCase.exception = BusinessException(AuthErrorCode.INVALID_TOKEN)

        mockMvc
            .post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_TOKEN") }
            }
    }

    @Test
    fun `이미 가입된 회원이면 409와 ALREADY_REGISTERED로 응답한다`() {
        fakeSignupUseCase.exception = BusinessException(AuthErrorCode.ALREADY_REGISTERED)

        mockMvc
            .post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("ALREADY_REGISTERED") }
            }
    }
}
