package parfait.core.notification.port.out

import parfait.core.notification.domain.MulticastResult
import parfait.core.notification.domain.PushMessage

interface NotificationSenderPort {
    /**
     * 단건 발송. 성공하면 반환값 없음, 실패하면
     * [parfait.core.notification.exception.NotificationSendException] 을 던진다
     * (retryable/errorCode 로 호출자가 재시도 여부를 판단).
     */
    fun send(
        token: String,
        message: PushMessage,
    )

    /**
     * 멀티캐스트 발송. tokens 는 최대 500개 (FCM sendEachForMulticast 상한 — 초과 시 호출자가 청킹).
     * 개별 토큰 실패는 예외를 던지지 않고 [MulticastResult] 에 담아 반환한다.
     * 요청 전체 실패(인증·쿼터 등)만 [parfait.core.notification.exception.NotificationSendException] 을 던진다.
     */
    fun sendMulticast(
        tokens: List<String>,
        message: PushMessage,
    ): MulticastResult
}
