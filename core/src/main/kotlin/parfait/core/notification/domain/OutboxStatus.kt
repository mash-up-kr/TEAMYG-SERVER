package parfait.core.notification.domain

enum class OutboxStatus { PENDING, SENT, FAILED }
// SUPPRESSED, CAPPED 는 후속 스로틀 정책 스펙에서 추가 (status VARCHAR(20) 이라 마이그레이션 불필요)
