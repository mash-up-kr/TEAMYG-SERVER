package parfait.core.notification.port.out

import java.time.LocalDateTime

interface NotificationOutboxPurgePort {
    /** status IN (SENT, FAILED) AND created_at < cutoff 인 행 삭제. 삭제된 행 수 반환. */
    fun deleteTerminalBefore(cutoff: LocalDateTime): Int
}
