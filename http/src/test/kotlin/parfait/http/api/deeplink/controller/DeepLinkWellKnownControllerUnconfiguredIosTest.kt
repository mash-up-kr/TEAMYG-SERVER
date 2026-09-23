package parfait.http.api.deeplink.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

// iOS Team ID/Bundle ID, Android 서명 지문이 아직 TODO(빈 문자열)인 현재 단계의 동작을 검증한다.
@WebMvcTest(controllers = [DeepLinkWellKnownController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class, TestMemberQueryPortConfig::class, TestTokenValidatePortConfig::class)
@TestPropertySource(properties = ["deeplink.android.package-name=com.teamyg.parfait"])
class DeepLinkWellKnownControllerUnconfiguredIosTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `서명 지문이 설정되지 않으면 빈 배열을 반환한다`() {
        mockMvc
            .get("/.well-known/assetlinks.json")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].target.sha256_cert_fingerprints.length()") { value(0) }
            }
    }

    @Test
    fun `iOS Team ID·Bundle ID가 설정되지 않으면 details가 빈 배열이다`() {
        mockMvc
            .get("/.well-known/apple-app-site-association")
            .andExpect {
                status { isOk() }
                jsonPath("$.applinks.details.length()") { value(0) }
            }
    }
}
