package parfait.http.api.landing

import io.kotest.matchers.shouldBe
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

@WebMvcTest(controllers = [RootLandingController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class, TestMemberQueryPortConfig::class, TestTokenValidatePortConfig::class)
@TestPropertySource(properties = ["landing.domain=parfait-app.store"])
class RootLandingControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `parfait-app-store로 요청하면 hello world를 응답한다`() {
        val response =
            mockMvc
                .get("/") {
                    with {
                        it.serverName = "parfait-app.store"
                        it
                    }
                }.andReturn()
                .response

        response.status shouldBe 200
        response.contentAsString shouldBe "hello world"
    }

    @Test
    fun `다른 호스트로 요청하면 404를 응답한다`() {
        val response =
            mockMvc
                .get("/") {
                    with {
                        it.serverName = "api.parfait-app.store"
                        it
                    }
                }.andReturn()
                .response

        response.status shouldBe 404
    }
}
