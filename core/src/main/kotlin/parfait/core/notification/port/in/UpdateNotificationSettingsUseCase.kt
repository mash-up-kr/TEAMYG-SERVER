@file:Suppress("ktlint:standard:package-name")

package parfait.core.notification.port.`in`

import parfait.core.notification.domain.NotificationSettings

interface UpdateNotificationSettingsUseCase {
    fun update(command: UpdateNotificationSettingsCommand): NotificationSettings
}

/** null 인 항목은 바꾸지 않는다(부분 변경). */
data class UpdateNotificationSettingsCommand(
    val memberId: Long,
    val dailyReminderEnabled: Boolean?,
    val activityDigestEnabled: Boolean?,
)
