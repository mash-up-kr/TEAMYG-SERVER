@file:Suppress("ktlint:standard:package-name")

package parfait.core.notification.port.`in`

import parfait.core.notification.domain.DevicePlatform

interface RegisterDeviceTokenUseCase {
    fun register(command: RegisterDeviceTokenCommand)
}

data class RegisterDeviceTokenCommand(
    val memberId: Long,
    val sessionId: String?,
    val token: String,
    val platform: DevicePlatform,
)
