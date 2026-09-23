package parfait.core.deeplink.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import parfait.core.deeplink.domain.Platform
import parfait.core.deeplink.port.`in`.ResolveStoreFallbackCommand
import parfait.core.deeplink.port.`in`.ResolveStoreFallbackUseCase
import parfait.core.deeplink.port.`in`.StoreFallbackResult
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class ResolveStoreFallbackService(
    @Value("\${deeplink.android.package-name}") private val androidPackageName: String,
    @Value("\${deeplink.ios.app-store-id:}") private val iosAppStoreId: String,
) : ResolveStoreFallbackUseCase {
    override fun resolve(command: ResolveStoreFallbackCommand): StoreFallbackResult {
        val platform = Platform.fromUserAgent(command.userAgent)
        val redirectUrl =
            when (platform) {
                Platform.ANDROID -> buildPlayStoreUrl(command.originalQueryString)
                Platform.IOS -> buildAppStoreUrl()
                Platform.OTHER -> null
            }
        return StoreFallbackResult(platform = platform, redirectUrl = redirectUrl)
    }

    // Play Store가 설치 후 첫 실행 시 Install Referrer API로 그대로 넘겨주는 값이다.
    private fun buildPlayStoreUrl(originalQueryString: String?): String {
        val base = "https://play.google.com/store/apps/details?id=$androidPackageName"
        val referrer = originalQueryString?.takeIf { it.isNotBlank() } ?: return base
        return "$base&referrer=${URLEncoder.encode(referrer, StandardCharsets.UTF_8)}"
    }

    private fun buildAppStoreUrl(): String? =
        iosAppStoreId.takeIf { it.isNotBlank() }?.let { "https://apps.apple.com/app/id$it" }
}
