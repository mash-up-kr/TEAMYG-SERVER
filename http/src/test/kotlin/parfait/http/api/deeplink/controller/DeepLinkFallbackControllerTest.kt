package parfait.http.api.deeplink.controller

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.core.deeplink.domain.Platform
import parfait.core.deeplink.port.`in`.DownloadStoreLinkResult
import parfait.core.deeplink.port.`in`.GetDownloadStoreLinkCommand
import parfait.core.deeplink.port.`in`.GetDownloadStoreLinkUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [DeepLinkFallbackController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
    DeepLinkFallbackControllerTest.FakeGetDownloadStoreLinkUseCaseConfig::class,
)
class DeepLinkFallbackControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var fakeUseCase: FakeGetDownloadStoreLinkUseCase

    @TestConfiguration
    class FakeGetDownloadStoreLinkUseCaseConfig {
        @Bean
        fun getDownloadStoreLinkUseCase(): FakeGetDownloadStoreLinkUseCase = FakeGetDownloadStoreLinkUseCase()
    }

    class FakeGetDownloadStoreLinkUseCase : GetDownloadStoreLinkUseCase {
        var result: DownloadStoreLinkResult = DownloadStoreLinkResult(platform = Platform.OTHER, redirectUrl = null)
        var lastCommand: GetDownloadStoreLinkCommand? = null

        override fun getDownloadLink(command: GetDownloadStoreLinkCommand): DownloadStoreLinkResult {
            lastCommand = command
            return result
        }
    }

    @Test
    fun `HTML 응답을 반환하고 원본 쿼리스트링을 UseCase에 그대로 전달한다`() {
        fakeUseCase.result =
            DownloadStoreLinkResult(
                platform = Platform.ANDROID,
                redirectUrl = "https://play.google.com/store/apps/details?id=com.teamyg.parfait",
            )

        val response =
            mockMvc
                .get("/link?link_id=abc&campaign=summer") {
                    header("User-Agent", "Android")
                }.andExpect {
                    status { isOk() }
                    content { contentType("${MediaType.TEXT_HTML_VALUE};charset=UTF-8") }
                }.andReturn()
                .response
                .contentAsString

        response shouldContain "play.google.com"

        val command = fakeUseCase.lastCommand
        checkNotNull(command)
        command.userAgent shouldBe "Android"
        command.originalQueryString shouldBe "link_id=abc&campaign=summer"
    }
}
