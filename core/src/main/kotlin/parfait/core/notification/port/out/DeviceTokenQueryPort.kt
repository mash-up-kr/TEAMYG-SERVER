package parfait.core.notification.port.out

import parfait.core.notification.domain.DeviceToken

interface DeviceTokenQueryPort {
    fun findByToken(token: String): DeviceToken?

    fun findByMemberId(memberId: Long): List<DeviceToken>
}
