@file:Suppress("ktlint:standard:package-name")

package parfait.core.notification.port.`in`

import parfait.core.notification.domain.NotificationSettings

interface GetNotificationSettingsUseCase {
    fun get(memberId: Long): NotificationSettings
}
