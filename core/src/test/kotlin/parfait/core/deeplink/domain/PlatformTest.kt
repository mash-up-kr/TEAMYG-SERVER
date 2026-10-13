package parfait.core.deeplink.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PlatformTest {
    @Test
    fun `User-Agent에 android가 포함되면 ANDROID를 반환한다`() {
        Platform.fromUserAgent("Mozilla/5.0 (Linux; Android 14; Pixel 8)") shouldBe Platform.ANDROID
    }

    @Test
    fun `User-Agent에 iPhone이 포함되면 IOS를 반환한다`() {
        Platform.fromUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)") shouldBe Platform.IOS
    }

    @Test
    fun `User-Agent에 iPad가 포함되면 IOS를 반환한다`() {
        Platform.fromUserAgent("Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X)") shouldBe Platform.IOS
    }

    @Test
    fun `데스크톱 User-Agent면 OTHER를 반환한다`() {
        Platform.fromUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15)") shouldBe Platform.OTHER
    }

    @Test
    fun `android와 iPhone이 함께 포함되면 선언 순서상 먼저인 ANDROID를 반환한다`() {
        Platform.fromUserAgent("Mozilla/5.0 (Android; iPhone)") shouldBe Platform.ANDROID
    }

    @Test
    fun `User-Agent가 없으면 OTHER를 반환한다`() {
        Platform.fromUserAgent(null) shouldBe Platform.OTHER
    }
}
