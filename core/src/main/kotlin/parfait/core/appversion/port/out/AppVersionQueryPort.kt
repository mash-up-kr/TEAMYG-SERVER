package parfait.core.appversion.port.out

import parfait.core.appversion.domain.AppPlatform
import parfait.core.appversion.domain.AppVersion

interface AppVersionQueryPort {
    fun findByPlatform(platform: AppPlatform): AppVersion?
}
