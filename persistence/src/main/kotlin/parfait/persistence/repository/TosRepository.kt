package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import parfait.persistence.entity.Tos

interface TosRepository : JpaRepository<Tos, Long> {
    @Query(
        value = """
            SELECT ranked.* FROM (
                SELECT t.*, ROW_NUMBER() OVER (PARTITION BY t.type ORDER BY t.published_at DESC, t.id DESC) AS rn
                FROM tos t
            ) ranked
            WHERE ranked.rn = 1
        """,
        nativeQuery = true,
    )
    fun findCurrentTerms(): List<Tos>
}
