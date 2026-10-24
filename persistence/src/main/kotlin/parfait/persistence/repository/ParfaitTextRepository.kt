package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.persistence.entity.ParfaitText

interface ParfaitTextRepository : JpaRepository<ParfaitText, Long> {
    fun findAllByParfaitIdOrderByPositionZAscIdAsc(parfaitId: Long): List<ParfaitText>

    fun existsByParfaitId(parfaitId: Long): Boolean
}
