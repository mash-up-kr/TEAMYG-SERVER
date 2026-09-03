package parfait.external.notification

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import org.springframework.stereotype.Component
import parfait.core.notification.domain.PushMessage
import parfait.core.notification.exception.NotificationSendException
import parfait.core.notification.port.out.NotificationSenderPort
import java.time.Instant

@Component
class FcmNotificationSender(
    private val firebaseMessaging: FirebaseMessaging,
) : NotificationSenderPort {
    private companion object {
        const val ANDROID_CHANNEL_ID = "parfait_default"
        val RETRYABLE_CODES =
            setOf(
                MessagingErrorCode.UNAVAILABLE,
                MessagingErrorCode.INTERNAL,
                MessagingErrorCode.QUOTA_EXCEEDED,
            )
    }

    override fun send(
        token: String,
        message: PushMessage,
    ) {
        val android =
            AndroidConfig
                .builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder().setChannelId(ANDROID_CHANNEL_ID).build())
                .apply { message.ttl?.let { setTtl(it.toMillis()) } }
                .build()

        val apns =
            ApnsConfig
                .builder()
                .putHeader("apns-priority", "10")
                .apply {
                    message.ttl?.let {
                        putHeader(
                            "apns-expiration",
                            Instant
                                .now()
                                .plus(it)
                                .epochSecond
                                .toString(),
                        )
                    }
                }.setAps(Aps.builder().setSound("default").build())
                .build()

        val fcm =
            Message
                .builder()
                .setToken(token)
                .setNotification(
                    Notification
                        .builder()
                        .setTitle(message.title)
                        .setBody(message.body)
                        .build(),
                ).putAllData(message.data)
                .setAndroidConfig(android)
                .setApnsConfig(apns)
                .build()

        try {
            firebaseMessaging.send(fcm)
        } catch (e: FirebaseMessagingException) {
            throw NotificationSendException(
                // 코드 미상(소켓/읽기 타임아웃·IOException·상세 없는 5xx → messagingErrorCode == null)은
                // 재시도 대상으로 본다. MAX_ATTEMPTS 로 상한이 걸린다.
                retryable = e.messagingErrorCode?.let { it in RETRYABLE_CODES } ?: true,
                errorCode = e.messagingErrorCode?.name,
                message = "FCM 발송 실패: ${e.messagingErrorCode} ${e.message}",
                cause = e,
            )
        }
    }
}
