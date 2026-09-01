package parfait.core.notification.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.notification.domain.DeviceToken
import parfait.core.notification.port.`in`.RegisterDeviceTokenCommand
import parfait.core.notification.port.`in`.RegisterDeviceTokenUseCase
import parfait.core.notification.port.out.DeviceTokenQueryPort
import parfait.core.notification.port.out.DeviceTokenSavePort

@Service
class RegisterDeviceTokenService(
    private val deviceTokenQueryPort: DeviceTokenQueryPort,
    private val deviceTokenSavePort: DeviceTokenSavePort,
) : RegisterDeviceTokenUseCase {
    @Transactional
    override fun register(command: RegisterDeviceTokenCommand) {
        val deviceToken =
            deviceTokenQueryPort.findByToken(command.token)?.apply {
                reassign(command.memberId, command.sessionId, command.platform)
            } ?: DeviceToken.register(
                memberId = command.memberId,
                sessionId = command.sessionId,
                token = command.token,
                platform = command.platform,
            )
        deviceTokenSavePort.save(deviceToken)
    }
}
