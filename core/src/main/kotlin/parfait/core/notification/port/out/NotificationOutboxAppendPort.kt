package parfait.core.notification.port.out

import parfait.core.notification.domain.NotificationOutbox

interface NotificationOutboxAppendPort {
    /** place() 트랜잭션 안에서 수신자당 1행 저장. dedup_key 충돌은 조용히 무시(생산자 멱등). */
    fun saveAll(messages: List<NotificationOutbox>)
}
