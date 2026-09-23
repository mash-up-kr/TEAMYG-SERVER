@file:Suppress("ktlint:standard:package-name")

package parfait.core.deeplink.port.`in`

import parfait.core.deeplink.domain.Platform

interface ResolveStoreFallbackUseCase {
    fun resolve(command: ResolveStoreFallbackCommand): StoreFallbackResult
}

data class ResolveStoreFallbackCommand(
    val userAgent: String?,
    val originalQueryString: String?,
)

data class StoreFallbackResult(
    val platform: Platform,
    val redirectUrl: String?,
)
