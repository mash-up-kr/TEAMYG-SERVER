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

    @Test
    fun `카카오톡 인앱 브라우저면 외부 브라우저 전환 URL과 안내 문구를 응답한다`() {
        fakeUseCase.result =
            DownloadStoreLinkResult(
                platform = Platform.ANDROID,
                redirectUrl = OPEN_EXTERNAL_URL,
                isInAppBrowser = true,
            )

        val response =
            mockMvc
                .get("/link?inviteCode=ABC123") {
                    header("User-Agent", "Mozilla/5.0 (Linux; Android 14) KAKAOTALK 2511290")
                }.andExpect {
                    status { isOk() }
                }.andReturn()
                .response
                .contentAsString

        response shouldContain
            "kakaotalk://web/openExternal?url=https%3A%2F%2Fparfait-app.store%2Flink%3FinviteCode%3DABC123"
        response shouldContain "외부 브라우저로 이동 중입니다"
    }

    @Test
    fun `공유 미리보기를 위한 OG 메타 태그를 응답에 포함한다`() {
        val response =
            mockMvc
                .get("/link?inviteCode=ABC123") {
                    header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                }.andExpect {
                    status { isOk() }
                }.andReturn()
                .response
                .contentAsString

        response shouldContain """<meta property="og:title" content="Parfait" />"""
        response shouldContain """<meta property="og:description" content="우리가 쌓을 하루, 사진 공유 캔버스 SNS" />"""
        response shouldContain
            """<meta property="og:image" content="https://parfait-app.store/assets/og_image.png" />"""
    }

    private companion object {
        const val OPEN_EXTERNAL_URL =
            "kakaotalk://web/openExternal?url=https%3A%2F%2Fparfait-app.store%2Flink%3FinviteCode%3DABC123"
    }
}
