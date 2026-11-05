package parfait.core.notification.port.out

interface NotificationSettingsUpdatePort {
    /** null 인 항목은 변경하지 않는다. */
    fun update(
        memberId: Long,
        dailyReminderEnabled: Boolean?,
        activityDigestEnabled: Boolean?,
    )
}
