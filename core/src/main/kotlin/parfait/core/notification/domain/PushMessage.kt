package parfait.core.notification.domain

import java.time.Duration

/**
 * 플랫폼 중립 푸시 메시지. FCM/Android/APNs 세부(채널 id, priority, sound 등)를 몰라야 한다 —
 * 그 조립은 external 의 FcmNotificationSender 가 상수로 담당한다.
 */
data class PushMessage(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val ttl: Duration? = null,
)
