package parfait.http.api.notification.dto

/** 보낸 항목만 바꾼다. null(미전송)은 변경하지 않는다. */
data class UpdateNotificationSettingsRequest(
    val dailyReminderEnabled: Boolean? = null,
    val activityDigestEnabled: Boolean? = null,
)
