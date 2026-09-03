package parfait.core.notification.exception

/** FcmNotificationSender 가 발송 실패 시 던진다. 호출자(디스패처)가 retryable 로 재시도 여부 판단. */
class NotificationSendException(
    val retryable: Boolean,
    val errorCode: String?,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
