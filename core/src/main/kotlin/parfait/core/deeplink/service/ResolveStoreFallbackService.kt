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

    // Play Store는 설치 후 최초 실행 시 이 referrer 값을 그대로 Install Referrer API로 넘겨준다.
    // 원본 쿼리스트링을 통째로 인코딩해 넣어야 Android 쪽에서 link_id/campaign 등을 복원할 수 있다.
    private fun buildPlayStoreUrl(originalQueryString: String?): String {
        val base = "https://play.google.com/store/apps/details?id=$androidPackageName"
        val referrer = originalQueryString?.takeIf { it.isNotBlank() } ?: return base
        return "$base&referrer=${URLEncoder.encode(referrer, StandardCharsets.UTF_8)}"
    }

    private fun buildAppStoreUrl(): String? =
        iosAppStoreId.takeIf { it.isNotBlank() }?.let { "https://apps.apple.com/app/id$it" }
}
