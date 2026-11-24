@file:Suppress("ktlint:standard:package-name")

package parfait.core.appversion.port.`in`

import parfait.core.appversion.domain.AppPlatform
import parfait.core.appversion.domain.UpdateType

data class AppVersionCheckResult(
    val updateType: UpdateType,
    val minimumVersion: String,
    val latestVersion: String,
    val storeUrl: String?,
)

interface CheckAppVersionUseCase {
    fun check(
        platform: AppPlatform,
        version: String,
    ): AppVersionCheckResult
}
