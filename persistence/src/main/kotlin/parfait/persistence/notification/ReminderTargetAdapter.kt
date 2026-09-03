package parfait.persistence.notification

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.notification.port.out.ReminderTargetQueryPort
import parfait.persistence.repository.DeviceTokenRepository

@Component
class ReminderTargetAdapter(
    private val deviceTokenRepository: DeviceTokenRepository,
) : ReminderTargetQueryPort {
    @Transactional(readOnly = true)
    override fun findActiveGroupMemberDeviceTokens(): List<String> = deviceTokenRepository.findActiveGroupMemberTokens()
}
