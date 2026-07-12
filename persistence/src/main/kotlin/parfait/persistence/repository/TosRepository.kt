package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.persistence.entity.Tos

interface TosRepository : JpaRepository<Tos, Long>
