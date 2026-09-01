package parfait.core.notification.port.out

import parfait.core.notification.domain.DeviceToken

interface DeviceTokenSavePort {
    fun save(deviceToken: DeviceToken): DeviceToken
}
