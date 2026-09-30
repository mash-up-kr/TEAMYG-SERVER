@file:Suppress("ktlint:standard:package-name")

package parfait.core.deeplink.port.`in`

import parfait.core.deeplink.domain.Platform

interface GetDownloadStoreLinkUseCase {
    fun getDownloadLink(command: GetDownloadStoreLinkCommand): DownloadStoreLinkResult
}

data class GetDownloadStoreLinkCommand(
    val userAgent: String?,
    val originalQueryString: String?,
)

data class DownloadStoreLinkResult(
    val platform: Platform,
    val redirectUrl: String?,
)
