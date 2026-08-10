package parfait.http.global.actuator

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.http.TestApplication
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
import kotlin.test.Test

@SpringBootTest(classes = [TestApplication::class])
@AutoConfigureMockMvc
@Import(
    TestMemberQueryPortConfig::class,
    TestKakaoLoginUseCaseConfig::class,
    TestParfaitGroupUseCaseConfig::class,
    TestSignupUseCaseConfig::class,
    TestReissueUseCaseConfig::class,
    TestLogoutUseCaseConfig::class,
    TestParfaitUseCaseConfig::class,
    TestPolicyQueryUseCaseConfig::class,
    TestChangeGlobalNicknameUseCaseConfig::class,
    TestIssueImageUploadUrlUseCaseConfig::class,
    TestConfirmImageUploadUseCaseConfig::class,
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
