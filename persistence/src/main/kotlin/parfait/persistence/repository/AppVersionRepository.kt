package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.core.appversion.domain.AppPlatform
import parfait.persistence.entity.AppVersionEntity

interface AppVersionRepository : JpaRepository<AppVersionEntity, Long> {
    fun findByPlatform(platform: AppPlatform): AppVersionEntity?
}
