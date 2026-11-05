package parfait.core.notification.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.notification.domain.NotificationSettings
import parfait.core.notification.port.`in`.GetNotificationSettingsUseCase
import parfait.core.notification.port.`in`.UpdateNotificationSettingsCommand
import parfait.core.notification.port.`in`.UpdateNotificationSettingsUseCase
import parfait.core.notification.port.out.NotificationSettingsQueryPort
import parfait.core.notification.port.out.NotificationSettingsUpdatePort

@Service
class NotificationSettingsService(
    private val queryPort: NotificationSettingsQueryPort,
    private val updatePort: NotificationSettingsUpdatePort,
) : GetNotificationSettingsUseCase,
    UpdateNotificationSettingsUseCase {
    @Transactional(readOnly = true)
    override fun get(memberId: Long): NotificationSettings = queryPort.findByMemberId(memberId)

    @Transactional
    override fun update(command: UpdateNotificationSettingsCommand): NotificationSettings {
        if (command.dailyReminderEnabled == null && command.activityDigestEnabled == null) {
            return queryPort.findByMemberId(command.memberId)
        }

        updatePort.update(command.memberId, command.dailyReminderEnabled, command.activityDigestEnabled)
        return queryPort.findByMemberId(command.memberId)
    }
}
