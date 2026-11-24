package parfait.persistence.appversion

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.appversion.domain.AppPlatform
import parfait.core.appversion.domain.AppVersion
import parfait.core.appversion.domain.SemVer
import parfait.core.appversion.port.out.AppVersionQueryPort
import parfait.persistence.entity.AppVersionEntity
import parfait.persistence.repository.AppVersionRepository

@Component
@Transactional(readOnly = true)
class AppVersionAdapter(
    private val appVersionRepository: AppVersionRepository,
) : AppVersionQueryPort {
    override fun findByPlatform(platform: AppPlatform): AppVersion? =
        appVersionRepository.findByPlatform(platform)?.toDomain()

    private fun AppVersionEntity.toDomain(): AppVersion =
        AppVersion(
            platform = platform,
            minimumVersion = SemVer.parse(minimumVersion),
            latestVersion = SemVer.parse(latestVersion),
            storeUrl = storeUrl,
        )
}
