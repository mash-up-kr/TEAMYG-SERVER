package parfait.core.notification.port.out

import parfait.core.notification.domain.NotificationOutbox
import java.time.LocalDateTime

/**
 * 디스패처 경로. claimBatch(SELECT ... FOR UPDATE SKIP LOCKED)와 mark* UPDATE 가
 * 옵션 A에서 한 트랜잭션의 원자 단위라 조회/쓰기 분리 컨벤션의 예외로 한 포트에 묶는다.
 * 모든 메서드는 호출자(NotificationOutboxDispatcher)의 @Transactional 경계 안에서 실행돼야 한다.
 */
interface NotificationOutboxPollPort {
    fun claimBatch(
        limit: Int,
        now: LocalDateTime,
    ): List<NotificationOutbox>

    fun markSent(
        id: Long,
        now: LocalDateTime,
        note: String?,
    )

    fun markRetry(
        id: Long,
        attempts: Int,
        scheduledAt: LocalDateTime,
        error: String,
    )

    fun markFailed(
        id: Long,
        error: String,
    )
}
