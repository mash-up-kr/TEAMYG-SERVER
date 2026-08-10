package parfait.http.global.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.http.TestApplication
import parfait.http.auth.controller.TestAppleLoginUseCaseConfig
import parfait.http.auth.controller.TestKakaoLoginUseCaseConfig
import parfait.http.auth.controller.TestLogoutUseCaseConfig
import parfait.http.auth.controller.TestPolicyQueryUseCaseConfig
import parfait.http.auth.controller.TestReissueUseCaseConfig
import parfait.http.auth.controller.TestSignupUseCaseConfig
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.image.controller.TestConfirmImageUploadUseCaseConfig
import parfait.http.image.controller.TestIssueImageUploadUrlUseCaseConfig
import parfait.http.member.controller.TestChangeGlobalNicknameUseCaseConfig
import parfait.http.parfait.controller.TestParfaitUseCaseConfig
import parfait.http.parfaitgroup.controller.TestParfaitGroupUseCaseConfig
import parfait.http.parfaitimage.controller.TestPlaceParfaitImageUseCaseConfig
import parfait.http.parfaitimage.controller.TestUpdateParfaitImageUseCaseConfig
import kotlin.test.Test

@SpringBootTest(classes = [TestApplication::class])
@AutoConfigureMockMvc
@Import(
    TestMemberQueryPortConfig::class,
    TestKakaoLoginUseCaseConfig::class,
    TestAppleLoginUseCaseConfig::class,
    TestSignupUseCaseConfig::class,
    TestParfaitGroupUseCaseConfig::class,
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
class OpenApiEndpointTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `OpenAPI 문서에 health API가 포함된다`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.info.title") { value("Parfait API") }
            jsonPath("$.info.version") { value("v1") }
            jsonPath("$.paths['/health']") { exists() }
        }
    }
}
