package parfait.core.notification.domain

/** S-105 알림 설정. 두 값은 서로 독립이다. */
data class NotificationSettings(
    val dailyReminderEnabled: Boolean,
    val activityDigestEnabled: Boolean,
) {
    companion object {
        val DEFAULT = NotificationSettings(dailyReminderEnabled = true, activityDigestEnabled = false)
    }
}
