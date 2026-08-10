package parfait.http.global.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.member.port.out.MemberQueryPort
import parfait.http.TestApplication
import parfait.http.auth.controller.TestAppleLoginUseCaseConfig
import parfait.http.auth.controller.TestKakaoLoginUseCaseConfig
import parfait.http.auth.controller.TestLogoutUseCaseConfig
import parfait.http.auth.controller.TestPolicyQueryUseCaseConfig
import parfait.http.auth.controller.TestReissueUseCaseConfig
import parfait.http.auth.controller.TestSignupUseCaseConfig
import parfait.http.image.controller.TestConfirmImageUploadUseCaseConfig
import parfait.http.image.controller.TestIssueImageUploadUrlUseCaseConfig
import parfait.http.member.controller.TestChangeGlobalNicknameUseCaseConfig
import parfait.http.parfait.controller.TestParfaitUseCaseConfig
import parfait.http.parfaitgroup.controller.TestParfaitGroupUseCaseConfig
import parfait.http.parfaitimage.controller.TestPlaceParfaitImageUseCaseConfig
import parfait.http.parfaitimage.controller.TestUpdateParfaitImageUseCaseConfig
import kotlin.test.Test

@SpringBootTest(
    classes = [TestApplication::class],
    properties = [
        "jwt.secret-key=integration-test-jwt-secret-key-32-bytes-min!!",
        "jwt.access-token-expiration-seconds=3600",
        "jwt.refresh-token-expiration-seconds=1209600",
        "jwt.registration-token-expiration-seconds=600",
    ],
)
@AutoConfigureMockMvc
@Import(
    SecurityConfigIntegrationTest.MemberOneOnlyExistsConfig::class,
    SecurityConfigIntegrationTest.DummyProtectedController::class,
    TestParfaitGroupUseCaseConfig::class,
    TestKakaoLoginUseCaseConfig::class,
    TestAppleLoginUseCaseConfig::class,
    TestSignupUseCaseConfig::class,
    TestReissueUseCaseConfig::class,
    TestLogoutUseCaseConfig::class,
    TestParfaitUseCaseConfig::class,
    TestPolicyQueryUseCaseConfig::class,
    TestChangeGlobalNicknameUseCaseConfig::class,
    TestIssueImageUploadUrlUseCaseConfig::class,
    TestConfirmImageUploadUseCaseConfig::class,
    TestPlaceParfaitImageUseCaseConfig::class,
    TestUpdateParfaitImageUseCaseConfig::class,
)
class SecurityConfigIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var tokenIssuePort: TokenIssuePort

    /**
     * 이 통합 테스트 전용 스텁이다. 이 스텁은 memberId==1L일 때만 true를
     * 반환해 "존재하지 않는 회원" 시나리오를 검증할 수 있어야 한다.
     */
    @TestConfiguration
    class MemberOneOnlyExistsConfig {
        @Bean
        fun memberQueryPort(): MemberQueryPort =
            object : MemberQueryPort {
                override fun existsById(memberId: Long): Boolean = memberId == 1L

                override fun findMemberIdByProvider(
                    provider: LoginProvider,
                    providerUserId: String,
                ): Long? = null

                override fun findGlobalNicknameById(memberId: Long): String? = if (memberId == 1L) "테스트" else null
            }
    }

    @RestController
    class DummyProtectedController {
        @GetMapping("/test/protected")
        fun protectedEndpoint(): String = SecurityContextHolder.getContext().authentication!!.name
    }

    @Test
    fun `whitelist 경로는 토큰 없이 통과한다`() {
        mockMvc.get("/actuator/health").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `swagger-ui html 경로는 토큰 없이 통과한다`() {
        mockMvc.get("/swagger-ui.html").andExpect {
            status { is3xxRedirection() }
        }
    }

    @Test
    fun `헬스체크 엔드포인트는 화이트리스트에 포함되어 토큰 없이 통과한다`() {
        mockMvc.get("/health").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `Authorization 헤더가 없으면 401과 UNAUTHORIZED로 응답한다`() {
        mockMvc.get("/test/protected").andExpect {
            status { isUnauthorized() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun `서명이 위조된 토큰이면 401과 INVALID_TOKEN으로 응답한다`() {
        mockMvc
            .get("/test/protected") {
                header("Authorization", "Bearer not-a-valid-jwt")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_TOKEN") }
            }
    }

    @Test
    fun `존재하지 않는 회원의 토큰이면 401과 MEMBER_NOT_FOUND로 응답한다`() {
        val token = tokenIssuePort.createAccessToken(memberId = 999L)

        mockMvc
            .get("/test/protected") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("MEMBER_NOT_FOUND") }
            }
    }

    @Test
    fun `존재하는 회원의 유효한 토큰이면 인증에 성공해 200을 응답한다`() {
        val token = tokenIssuePort.createAccessToken(memberId = 1L)

        mockMvc
            .get("/test/protected") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                content { string("1") }
            }
    }

    @Test
    fun `카카오 로그인 엔드포인트는 화이트리스트에 포함되어 토큰 없이 통과한다`() {
        mockMvc
            .post("/api/v1/auth/kakao") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"idToken":"dummy","nonce":"dummy"}"""
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `애플 로그인 엔드포인트는 화이트리스트에 포함되어 토큰 없이 통과한다`() {
        mockMvc
            .post("/api/v1/auth/apple") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"identityToken":"dummy","nonce":"dummy","authorizationCode":"dummy"}"""
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `회원가입 엔드포인트는 화이트리스트에 포함되어 토큰 없이 통과한다`() {
        mockMvc
            .post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"registrationToken":"dummy","agreements":[{"termsId":1,"agreed":true}]}"""
            }.andExpect {
                status { isCreated() }
            }
    }

    @Test
    fun `재발급 엔드포인트는 화이트리스트에 포함되어 토큰 없이 통과한다`() {
        mockMvc
            .post("/api/v1/auth/reissue") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"refreshToken":"dummy"}"""
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `약관 목록 엔드포인트는 화이트리스트에 포함되어 토큰 없이 통과한다`() {
        mockMvc.get("/api/v1/policies").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `로그아웃 엔드포인트는 화이트리스트가 아니라 유효한 토큰이 있어야 204를 응답한다`() {
        val token = tokenIssuePort.createAccessToken(memberId = 1L)

        mockMvc
            .post("/api/v1/auth/logout") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = """{"refreshToken":"refresh-token"}"""
            }.andExpect {
                status { isNoContent() }
            }
    }

    @Test
    fun `로그아웃 엔드포인트는 위조된 토큰이면 401과 INVALID_TOKEN으로 응답한다`() {
        mockMvc
            .post("/api/v1/auth/logout") {
                header("Authorization", "Bearer not-a-valid-jwt")
                contentType = MediaType.APPLICATION_JSON
                content = """{"refreshToken":"refresh-token"}"""
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_TOKEN") }
            }
    }

    @Test
    fun `로그아웃 엔드포인트는 Authorization 헤더가 없으면 401과 UNAUTHORIZED로 응답한다`() {
        mockMvc
            .post("/api/v1/auth/logout") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"refreshToken":"refresh-token"}"""
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("UNAUTHORIZED") }
            }
    }
}
