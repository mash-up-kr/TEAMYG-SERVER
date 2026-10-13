package parfait.http.api.deeplink.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [DeepLinkWellKnownController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class, TestMemberQueryPortConfig::class, TestTokenValidatePortConfig::class)
@TestPropertySource(
    properties = [
        "deeplink.android.package-name=com.teamyg.parfait",
        "deeplink.android.sha256-cert-fingerprints=AA:BB, CC:DD",
        "deeplink.ios.team-id=TEAMID123",
        "deeplink.ios.bundle-id=com.teamyg.parfait",
    ],
)
class DeepLinkWellKnownControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `assetlinks-json은 패키지명과 콤마 구분 지문 목록을 파싱해 반환한다`() {
        mockMvc
            .get("/.well-known/assetlinks.json")
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$[0].relation[0]") { value("delegate_permission/common.handle_all_urls") }
                jsonPath("$[0].target.namespace") { value("android_app") }
                jsonPath("$[0].target.package_name") { value("com.teamyg.parfait") }
                jsonPath("$[0].target.sha256_cert_fingerprints[0]") { value("AA:BB") }
                jsonPath("$[0].target.sha256_cert_fingerprints[1]") { value("CC:DD") }
            }
    }

    @Test
    fun `apple-app-site-association은 Team ID와 Bundle ID를 점으로 조합해 appID로 반환한다`() {
        mockMvc
            .get("/.well-known/apple-app-site-association")
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.applinks.details[0].appID") { value("TEAMID123.com.teamyg.parfait") }
                jsonPath("$.applinks.details[0].paths[0]") { value("/link") }
            }
    }
}
