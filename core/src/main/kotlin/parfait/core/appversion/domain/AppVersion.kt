package parfait.core.appversion.domain

data class AppVersion(
    val platform: AppPlatform,
    val minimumVersion: SemVer,
    val latestVersion: SemVer,
    val storeUrl: String?,
) {
    fun checkUpdateType(currentVersion: SemVer): UpdateType =
        when {
            currentVersion < minimumVersion -> UpdateType.FORCE
            currentVersion < latestVersion -> UpdateType.OPTIONAL
            else -> UpdateType.NONE
        }
}
