package parfait.persistence.stats

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.stats.domain.SignupStats
import parfait.core.stats.port.out.SignupStatsQueryPort
import java.time.LocalDateTime

/**
 * `Member` 엔티티에는 `@SQLRestriction("deleted_at IS NULL")` 이 걸려 있어 JPA 조회로는 탈퇴자가 보이지 않는다.
 * 그래서 탈퇴자까지 세려고 네이티브 쿼리를 쓴다.
 */
@Component
class SignupStatsAdapter(
    private val entityManager: EntityManager,
    @Value("\${report.stats.excluded-member-ids:}") excludedMemberIdsRaw: String,
) : SignupStatsQueryPort {
    private val excludedMemberIds: List<Long> =
        excludedMemberIdsRaw
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.toLongOrNull() ?: error("report.stats.excluded-member-ids 값이 올바르지 않습니다: '$it'") }

    @Transactional(readOnly = true)
    override fun aggregate(
        from: LocalDateTime,
        to: LocalDateTime,
    ): SignupStats {
        val exclusion = if (excludedMemberIds.isEmpty()) "" else "WHERE id NOT IN (:excludedIds)"
        val sql =
            """
            SELECT
              COALESCE(SUM(created_at >= :from AND created_at < :to), 0),
              COALESCE(SUM(deleted_at >= :from AND deleted_at < :to), 0),
              COALESCE(SUM(created_at >= :from AND created_at < :to
                           AND deleted_at >= :from AND deleted_at < :to), 0),
              COALESCE(SUM(created_at < :to AND (deleted_at IS NULL OR deleted_at >= :to)), 0)
            FROM member
            $exclusion
            """.trimIndent()

        val query =
            entityManager
                .createNativeQuery(sql)
                .setParameter("from", from)
                .setParameter("to", to)
        if (excludedMemberIds.isNotEmpty()) query.setParameter("excludedIds", excludedMemberIds)

        val row = query.singleResult as Array<*>
        return SignupStats(
            newSignups = (row[0] as Number).toLong(),
            withdrawals = (row[1] as Number).toLong(),
            newSignupWithdrawals = (row[2] as Number).toLong(),
            totalUsers = (row[3] as Number).toLong(),
        )
    }
}
