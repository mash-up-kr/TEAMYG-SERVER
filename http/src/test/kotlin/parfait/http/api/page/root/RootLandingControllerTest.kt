package parfait.http.api.page.root

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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

@WebMvcTest(controllers = [RootLandingController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class, TestMemberQueryPortConfig::class, TestTokenValidatePortConfig::class)
@TestPropertySource(properties = ["landing.domain=parfait-app.store"])
class RootLandingControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `parfait-app-store로 요청하면 랜딩 페이지 HTML을 응답한다`() {
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
        response.contentType shouldBe "${MediaType.TEXT_HTML_VALUE};charset=UTF-8"
        response.contentAsString shouldContain "landing-test-fixture"
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
