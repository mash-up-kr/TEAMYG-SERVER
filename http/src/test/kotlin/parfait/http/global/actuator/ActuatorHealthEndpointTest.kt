package parfait.http.global.actuator

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
import parfait.http.member.controller.TestGetMyAccountUseCaseConfig
import parfait.http.member.controller.TestWithdrawUseCaseConfig
import parfait.http.parfait.controller.TestParfaitUseCaseConfig
import parfait.http.parfait.controller.TestRotateParfaitCanvasesUseCaseConfig
import parfait.http.parfaitgroup.controller.TestParfaitGroupUseCaseConfig
import parfait.http.parfaitimage.controller.TestDeleteParfaitImageUseCaseConfig
import parfait.http.parfaitimage.controller.TestPlaceParfaitImageUseCaseConfig
import parfait.http.parfaitimage.controller.TestUpdateParfaitImageBorderUseCaseConfig
import parfait.http.parfaitimage.controller.TestUpdateParfaitImageUseCaseConfig
import kotlin.test.Test

@SpringBootTest(classes = [TestApplication::class])
@AutoConfigureMockMvc
@Import(
    TestMemberQueryPortConfig::class,
    TestKakaoLoginUseCaseConfig::class,
    TestAppleLoginUseCaseConfig::class,
    TestParfaitGroupUseCaseConfig::class,
    TestSignupUseCaseConfig::class,
    TestReissueUseCaseConfig::class,
    TestLogoutUseCaseConfig::class,
    TestParfaitUseCaseConfig::class,
    TestPolicyQueryUseCaseConfig::class,
    TestChangeGlobalNicknameUseCaseConfig::class,
    TestWithdrawUseCaseConfig::class,
    TestGetMyAccountUseCaseConfig::class,
    TestIssueImageUploadUrlUseCaseConfig::class,
    TestConfirmImageUploadUseCaseConfig::class,
    TestPlaceParfaitImageUseCaseConfig::class,
    TestUpdateParfaitImageUseCaseConfig::class,
    TestUpdateParfaitImageBorderUseCaseConfig::class,
    TestDeleteParfaitImageUseCaseConfig::class,
    TestRotateParfaitCanvasesUseCaseConfig::class,
)
class ActuatorHealthEndpointTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `actuator health 엔드포인트는 UP 상태를 응답한다`() {
        mockMvc.get("/actuator/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
        }
    }
}
