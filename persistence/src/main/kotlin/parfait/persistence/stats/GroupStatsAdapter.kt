package parfait.persistence.stats

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.stats.port.out.GroupStatsQueryPort
import java.time.LocalDateTime

/** 그룹에는 생성자 컬럼이 없어서 테스트 계정 제외(`report.stats.excluded-member-ids`)는 적용하지 않는다. */
@Component
class GroupStatsAdapter(
    private val entityManager: EntityManager,
) : GroupStatsQueryPort {
    @Transactional(readOnly = true)
    override fun countCreated(
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long =
        (
            entityManager
                .createNativeQuery("SELECT COUNT(*) FROM parfait_group WHERE created_at >= :from AND created_at < :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .singleResult as Number
        ).toLong()
}
