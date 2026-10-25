package parfait.http.api.deeplink.view

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import parfait.core.deeplink.domain.Platform

class DeepLinkFallbackPageRendererTest {
    @Test
    fun `로딩 표시와 안내 문구와 직접 이동하기 버튼을 렌더링한다`() {
        val html =
            DeepLinkFallbackPageRenderer.render(
                Platform.ANDROID,
                "https://play.google.com/store/apps/details?id=a&referrer=b",
                OG_IMAGE_URL,
            )

        html.split("""<span class="dot"></span>""").size - 1 shouldBe 3
        html shouldContain "파르페로 이동 중입니다"
        html shouldContain "잠시만 기다려 주세요..."
        html shouldContain
            """<a id="fallback-link" class="button" href="https://play.google.com/store/apps/details?id=a&amp;referrer=b">직접 이동하기</a>"""
    }

    @Test
    fun `이동할 URL이 없으면 직접 이동하기 버튼은 자리표시자 링크를 가진다`() {
        val html = DeepLinkFallbackPageRenderer.render(Platform.OTHER, null, OG_IMAGE_URL)

        html shouldContain """<a id="fallback-link" class="button" href="#">직접 이동하기</a>"""
    }

    private companion object {
        const val OG_IMAGE_URL = "https://parfait-app.store/assets/og_image.png"
    }
}
