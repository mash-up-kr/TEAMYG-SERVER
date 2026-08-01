package parfait.http.global.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.http.TestApplication
import parfait.http.api.auth.controller.TestKakaoLoginUseCaseConfig
import parfait.http.api.auth.controller.TestSignupUseCaseConfig
import parfait.http.api.parfait.controller.TestParfaitUseCaseConfig
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.parfaitgroup.TestParfaitGroupUseCaseConfig
import kotlin.test.Test

@SpringBootTest(classes = [TestApplication::class])
@AutoConfigureMockMvc
@Import(
    TestMemberQueryPortConfig::class,
    TestKakaoLoginUseCaseConfig::class,
    TestSignupUseCaseConfig::class,
    TestParfaitGroupUseCaseConfig::class,
    TestParfaitUseCaseConfig::class,
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
