package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.persistence.entity.ParfaitHistory

interface ParfaitHistoryRepository : JpaRepository<ParfaitHistory, Long>
