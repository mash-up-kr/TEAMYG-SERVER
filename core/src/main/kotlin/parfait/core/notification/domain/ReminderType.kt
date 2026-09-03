package parfait.core.notification.domain

/** 데일리 리마인드 종류. dataType 은 FCM data payload 의 "type" 값 (페이로드 스펙 v1 §3.1). */
enum class ReminderType(
    val dataType: String,
) {
    MORNING("REMIND_AM"), // P-02, 10:00 KST
    EVENING("REMIND_PM"), // P-03, 20:00 KST
}
