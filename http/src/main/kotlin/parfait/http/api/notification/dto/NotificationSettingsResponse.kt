package parfait.http.api.notification.dto

import parfait.core.notification.domain.NotificationSettings

data class NotificationSettingsResponse(
    val dailyReminderEnabled: Boolean,
    val activityDigestEnabled: Boolean,
) {
    companion object {
        fun from(settings: NotificationSettings) =
            NotificationSettingsResponse(
                dailyReminderEnabled = settings.dailyReminderEnabled,
                activityDigestEnabled = settings.activityDigestEnabled,
            )
    }
}
