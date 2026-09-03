package parfait.external.notification

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import parfait.core.notification.domain.FcmErrorCodes
import parfait.core.notification.domain.MulticastResult
import parfait.core.notification.domain.PushMessage
import parfait.core.notification.domain.TokenSendResult
import parfait.core.notification.exception.NotificationSendException
import parfait.core.notification.port.out.NotificationSenderPort
import java.time.Instant

@Component
class FcmNotificationSender(
    private val firebaseMessaging: FirebaseMessaging,
) : NotificationSenderPort {
    private val log = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val ANDROID_CHANNEL_ID = "parfait_default"
        const val MULTICAST_MAX = 500
    }

    override fun send(
        token: String,
        message: PushMessage,
    ) {
        val fcm =
            Message
                .builder()
                .setToken(token)
                .setNotification(notification(message))
                .putAllData(message.data)
                .setAndroidConfig(androidConfig(message))
                .setApnsConfig(apnsConfig(message))
                .build()

        try {
            firebaseMessaging.send(fcm)
        } catch (e: FirebaseMessagingException) {
            log.warn("FCM 단건 발송 실패 - errorCode={}", e.messagingErrorCode?.name)
            throw NotificationSendException(
                // 코드 미상(소켓/읽기 타임아웃·IOException·상세 없는 5xx → messagingErrorCode == null)은
                // 재시도 대상으로 본다. MAX_ATTEMPTS 로 상한이 걸린다.
                retryable = e.messagingErrorCode?.name?.let { it in FcmErrorCodes.RETRYABLE } ?: true,
                errorCode = e.messagingErrorCode?.name,
                message = "FCM 발송 실패: ${e.messagingErrorCode} ${e.message}",
                cause = e,
            )
        }
    }

    override fun sendMulticast(
        tokens: List<String>,
        message: PushMessage,
    ): MulticastResult {
        require(tokens.size <= MULTICAST_MAX) { "멀티캐스트 토큰은 최대 ${MULTICAST_MAX}개: ${tokens.size}" }

        val multicast =
            MulticastMessage
                .builder()
                .addAllTokens(tokens)
                .setNotification(notification(message))
                .putAllData(message.data)
                .setAndroidConfig(androidConfig(message))
                .setApnsConfig(apnsConfig(message))
                .build()

        val batch =
            try {
                firebaseMessaging.sendEachForMulticast(multicast)
            } catch (e: FirebaseMessagingException) {
                log.warn("FCM 멀티캐스트 전체 실패 - errorCode={} tokens={}", e.messagingErrorCode?.name, tokens.size)
                throw NotificationSendException(
                    retryable = e.messagingErrorCode?.name?.let { it in FcmErrorCodes.RETRYABLE } ?: true,
                    errorCode = e.messagingErrorCode?.name,
                    message = "FCM 멀티캐스트 전체 실패: ${e.messagingErrorCode} ${e.message}",
                    cause = e,
                )
            }

        check(batch.responses.size == tokens.size) {
            "FCM 멀티캐스트 응답 수 불일치: 요청 ${tokens.size}, 응답 ${batch.responses.size}"
        }

        val result =
            MulticastResult(
                batch.responses.mapIndexed { i, r ->
                    TokenSendResult(
                        token = tokens[i],
                        success = r.isSuccessful,
                        errorCode = (r.exception as? FirebaseMessagingException)?.messagingErrorCode?.name,
                    )
                },
            )
        if (result.failureCount > 0) {
            log.warn(
                "FCM 멀티캐스트 부분 실패 - 실패={}/{} 코드분포={}",
                result.failureCount,
                tokens.size,
                result.results
                    .filterNot { it.success }
                    .groupingBy { it.errorCode }
                    .eachCount(),
            )
        }
        return result
    }

    private fun notification(message: PushMessage): Notification =
        Notification
            .builder()
            .setTitle(message.title)
            .setBody(message.body)
            .build()

    private fun androidConfig(message: PushMessage): AndroidConfig =
        AndroidConfig
            .builder()
            .setPriority(AndroidConfig.Priority.HIGH)
            .setNotification(AndroidNotification.builder().setChannelId(ANDROID_CHANNEL_ID).build())
            .apply { message.ttl?.let { setTtl(it.toMillis()) } }
            .build()

    private fun apnsConfig(message: PushMessage): ApnsConfig =
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
}
