package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import parfait.persistence.entity.NotificationOutboxEntity
import java.time.LocalDateTime

interface NotificationOutboxRepository : JpaRepository<NotificationOutboxEntity, Long> {
    @Query(
        value = """
            SELECT * FROM notification_outbox
            WHERE status = 'PENDING' AND scheduled_at <= :now
            ORDER BY scheduled_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true,
    )
    fun claimBatch(
        @Param("now") now: LocalDateTime,
        @Param("limit") limit: Int,
    ): List<NotificationOutboxEntity>

    @Modifying
    @Query(
        value = """
            DELETE FROM notification_outbox
            WHERE status IN ('SENT', 'FAILED') AND created_at < :cutoff
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun deleteTerminalBefore(
        @Param("cutoff") cutoff: LocalDateTime,
        @Param("limit") limit: Int,
    ): Int
}
