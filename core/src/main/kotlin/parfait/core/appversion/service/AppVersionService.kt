package parfait.core.appversion.service

import org.springframework.stereotype.Service
import parfait.core.appversion.domain.AppPlatform
import parfait.core.appversion.domain.SemVer
import parfait.core.appversion.exception.AppVersionErrorCode
import parfait.core.appversion.port.`in`.AppVersionCheckResult
import parfait.core.appversion.port.`in`.CheckAppVersionUseCase
import parfait.core.appversion.port.out.AppVersionQueryPort
import parfait.core.exception.BusinessException

@Service
class AppVersionService(
    private val appVersionQueryPort: AppVersionQueryPort,
) : CheckAppVersionUseCase {
    override fun check(
        platform: AppPlatform,
        version: String,
    ): AppVersionCheckResult {
        val currentVersion =
            runCatching { SemVer.parse(version) }
                .getOrElse { throw BusinessException(AppVersionErrorCode.INVALID_VERSION_FORMAT) }

        val appVersion =
            appVersionQueryPort.findByPlatform(platform)
                ?: throw BusinessException(AppVersionErrorCode.APP_VERSION_NOT_FOUND)

        return AppVersionCheckResult(
            updateType = appVersion.checkUpdateType(currentVersion),
            minimumVersion = appVersion.minimumVersion.toString(),
            latestVersion = appVersion.latestVersion.toString(),
            storeUrl = appVersion.storeUrl,
        )
    }
}
