package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.persistence.entity.Parfait

interface ParfaitRepository : JpaRepository<Parfait, Long>
