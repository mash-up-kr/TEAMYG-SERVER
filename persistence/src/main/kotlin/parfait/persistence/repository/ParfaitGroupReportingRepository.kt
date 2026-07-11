package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.persistence.entity.ParfaitGroupReporting

interface ParfaitGroupReportingRepository : JpaRepository<ParfaitGroupReporting, Long>
