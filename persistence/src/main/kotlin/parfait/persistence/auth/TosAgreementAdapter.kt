package parfait.persistence.auth

import org.springframework.stereotype.Component
import parfait.core.auth.port.out.TosAgreementSavePort
import parfait.persistence.entity.TosAgreement
import parfait.persistence.repository.TosAgreementRepository

@Component
class TosAgreementAdapter(
    private val tosAgreementRepository: TosAgreementRepository,
) : TosAgreementSavePort {
    override fun saveAll(
        memberId: Long,
        tosIds: List<Long>,
    ) {
        tosAgreementRepository.saveAll(
            tosIds.map { tosId -> TosAgreement(memberId = memberId, tosId = tosId) },
        )
    }
}
