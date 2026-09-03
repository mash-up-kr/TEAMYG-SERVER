package parfait.core.notification.port.out

import parfait.core.notification.domain.PushMessage

interface NotificationSenderPort {
    /**
     * 단건 발송. 성공하면 반환값 없음, 실패하면
     * [parfait.core.notification.exception.NotificationSendException] 을 던진다
     * (retryable/errorCode 로 호출자가 재시도 여부를 판단).
     *
     * 멀티캐스트(sendMulticast)는 리마인드 배치 스펙에서 이 인터페이스에 추가한다.
     */
    fun send(
        token: String,
        message: PushMessage,
    )
}
