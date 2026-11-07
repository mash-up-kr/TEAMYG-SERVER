package parfait.core.notification.domain

enum class OutboxStatus { PENDING, SENT, FAILED, DEFERRED }
// DEFERRED: 알림 모아 받기 윈도우 안에서 발송을 미룬 활동 행. digest_ref 로 P-06 예약 행에 연결된다.
