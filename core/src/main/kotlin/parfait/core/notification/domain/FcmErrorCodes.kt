package parfait.core.notification.domain

/**
 * FCM 응답 에러코드 분류. P-01 디스패처와 리마인드 배치가 공유한다.
 *
 * 값은 `com.google.firebase.messaging.MessagingErrorCode` enum 상수명과 정확히 일치해야 한다
 * (core 는 firebase-admin 에 의존하지 않으므로 String 으로 든다 — 오타는 컴파일러가 못 잡고
 * external 테스트에서만 드러난다).
 */
object FcmErrorCodes {
    /** 이 코드로 실패한 토큰은 영구 무효 — 즉시 삭제 (정책 E-10/E-12). */
    val DEAD_TOKEN = setOf("UNREGISTERED", "INVALID_ARGUMENT", "SENDER_ID_MISMATCH")

    /** 토큰은 유효, 서버 측 일시 문제 — 재시도 가능. */
    val RETRYABLE = setOf("UNAVAILABLE", "INTERNAL", "QUOTA_EXCEEDED")
}
