package parfait.http.actuator

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.http.TestApplication
import parfait.http.auth.controller.TestKakaoLoginUseCaseConfig
import parfait.http.security.TestMemberQueryPortConfig
import kotlin.test.Test

@SpringBootTest(classes = [TestApplication::class])
@AutoConfigureMockMvc
@Import(TestMemberQueryPortConfig::class, TestKakaoLoginUseCaseConfig::class)
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
