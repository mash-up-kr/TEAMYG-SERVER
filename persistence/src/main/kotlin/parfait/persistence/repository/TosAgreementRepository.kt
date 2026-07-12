package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.persistence.entity.TosAgreement

interface TosAgreementRepository : JpaRepository<TosAgreement, Long>
