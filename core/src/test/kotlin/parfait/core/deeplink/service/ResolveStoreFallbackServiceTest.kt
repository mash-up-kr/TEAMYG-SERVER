package parfait.core.deeplink.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import parfait.core.deeplink.domain.Platform
import parfait.core.deeplink.port.`in`.ResolveStoreFallbackCommand

class ResolveStoreFallbackServiceTest {
    @Test
    fun `Android User-Agent면 Play Store URL에 원본 쿼리스트링을 referrer로 인코딩해 넣는다`() {
        val service = ResolveStoreFallbackService(androidPackageName = "com.teamyg.parfait", iosAppStoreId = "")

        val result =
            service.resolve(
                ResolveStoreFallbackCommand(
                    userAgent = "Mozilla/5.0 (Linux; Android 14)",
                    originalQueryString = "link_id=abc&campaign=summer",
                ),
            )

        result.platform shouldBe Platform.ANDROID
        result.redirectUrl shouldBe
            "https://play.google.com/store/apps/details?id=com.teamyg.parfait&referrer=link_id%3Dabc%26campaign%3Dsummer"
    }

    @Test
    fun `Android User-Agent인데 쿼리스트링이 없으면 referrer 없이 기본 URL만 반환한다`() {
        val service = ResolveStoreFallbackService(androidPackageName = "com.teamyg.parfait", iosAppStoreId = "")

        val result =
            service.resolve(
                ResolveStoreFallbackCommand(userAgent = "Android", originalQueryString = null),
            )

        result.redirectUrl shouldBe "https://play.google.com/store/apps/details?id=com.teamyg.parfait"
    }

    @Test
    fun `iOS User-Agent이고 App Store ID가 설정되어 있으면 App Store URL을 반환한다`() {
        val service =
            ResolveStoreFallbackService(androidPackageName = "com.teamyg.parfait", iosAppStoreId = "1234567890")

        val result =
            service.resolve(
                ResolveStoreFallbackCommand(
                    userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",
                    originalQueryString = "link_id=abc",
                ),
            )

        result.platform shouldBe Platform.IOS
        result.redirectUrl shouldBe "https://apps.apple.com/app/id1234567890"
    }

    @Test
    fun `iOS User-Agent인데 App Store ID가 비어 있으면 redirectUrl이 null이다`() {
        val service = ResolveStoreFallbackService(androidPackageName = "com.teamyg.parfait", iosAppStoreId = "")

        val result =
            service.resolve(
                ResolveStoreFallbackCommand(
                    userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",
                    originalQueryString = null,
                ),
            )

        result.platform shouldBe Platform.IOS
        result.redirectUrl shouldBe null
    }

    @Test
    fun `데스크톱 User-Agent면 redirectUrl이 null이다`() {
        val service =
            ResolveStoreFallbackService(androidPackageName = "com.teamyg.parfait", iosAppStoreId = "1234567890")

        val result =
            service.resolve(
                ResolveStoreFallbackCommand(
                    userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15)",
                    originalQueryString = "link_id=abc",
                ),
            )

        result.platform shouldBe Platform.OTHER
        result.redirectUrl shouldBe null
    }
}
