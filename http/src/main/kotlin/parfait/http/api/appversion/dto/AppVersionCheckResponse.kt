package parfait.http.api.appversion.dto

import parfait.core.appversion.domain.UpdateType
import parfait.core.appversion.port.`in`.AppVersionCheckResult

data class AppVersionCheckResponse(
    val updateType: UpdateType,
    val minimumVersion: String,
    val latestVersion: String,
    val storeUrl: String?,
) {
    companion object {
        fun from(result: AppVersionCheckResult): AppVersionCheckResponse =
            AppVersionCheckResponse(
                updateType = result.updateType,
                minimumVersion = result.minimumVersion,
                latestVersion = result.latestVersion,
                storeUrl = result.storeUrl,
            )
    }
}
