package parfait.external.notification

import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.stereotype.Component
import parfait.core.notification.domain.PushMessage
import parfait.core.notification.port.out.NotificationSenderPort

@Component
class FcmNotificationSender(
    private val firebaseMessaging: FirebaseMessaging,
) : NotificationSenderPort {
    override fun send(
        token: String,
        message: PushMessage,
    ) {
        TODO("2026-09-02 스펙 Task B13: FCM 단건 발송 + Android/APNs 엔벨로프 + 에러코드 분기")
    }
}
