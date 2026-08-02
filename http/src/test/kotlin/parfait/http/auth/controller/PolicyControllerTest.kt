package parfait.http.auth.controller

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
import parfait.core.auth.domain.TosType
import parfait.core.auth.port.`in`.PolicyQueryUseCase
import parfait.core.auth.port.out.CurrentTerms
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [PolicyController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
    PolicyControllerTest.FakePolicyQueryUseCaseConfig::class,
)
class PolicyControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var fakePolicyQueryUseCase: FakePolicyQueryUseCase

    @BeforeEach
    fun resetFake() {
        fakePolicyQueryUseCase.result = emptyList()
    }

    @TestConfiguration
    class FakePolicyQueryUseCaseConfig {
        @Bean
        fun policyQueryUseCase(): FakePolicyQueryUseCase = FakePolicyQueryUseCase()
    }

    class FakePolicyQueryUseCase : PolicyQueryUseCase {
        var result: List<CurrentTerms> = emptyList()

        override fun getPolicies(): List<CurrentTerms> = result
    }

    @Test
    fun `약관 목록을 조회하면 200과 policies 배열을 응답한다`() {
        fakePolicyQueryUseCase.result =
            listOf(
                CurrentTerms(
                    id = 1L,
                    type = TosType.TERMS_OF_SERVICE,
                    title = "서비스 이용약관",
                    url = "https://parfait.app/policies/terms",
                    required = true,
                ),
                CurrentTerms(
                    id = 2L,
                    type = TosType.PRIVACY_POLICY,
                    title = "개인정보 처리방침",
                    url = "https://parfait.app/policies/privacy",
                    required = true,
                ),
            )

        mockMvc.get("/api/v1/policies").andExpect {
            status { isOk() }
            jsonPath("$.data.policies[0].termsId") { value(1) }
            jsonPath("$.data.policies[0].type") { value("TERMS_OF_SERVICE") }
            jsonPath("$.data.policies[0].title") { value("서비스 이용약관") }
            jsonPath("$.data.policies[0].url") { value("https://parfait.app/policies/terms") }
            jsonPath("$.data.policies[0].required") { value(true) }
            jsonPath("$.data.policies[1].termsId") { value(2) }
            jsonPath("$.data.policies[1].type") { value("PRIVACY_POLICY") }
        }
    }

    @Test
    fun `현재 약관이 없으면 빈 policies 배열을 응답한다`() {
        mockMvc.get("/api/v1/policies").andExpect {
            status { isOk() }
            jsonPath("$.data.policies.length()") { value(0) }
        }
    }
}
