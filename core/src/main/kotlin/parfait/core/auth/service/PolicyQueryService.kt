package parfait.core.auth.service

import org.springframework.stereotype.Service
import parfait.core.auth.domain.TosType
import parfait.core.auth.port.`in`.PolicyQueryUseCase
import parfait.core.auth.port.out.CurrentTerms
import parfait.core.auth.port.out.TosQueryPort

@Service
class PolicyQueryService(
    private val tosQueryPort: TosQueryPort,
) : PolicyQueryUseCase {
    override fun getPolicies(): List<CurrentTerms> {
        val currentTerms = tosQueryPort.findCurrentTerms().associateBy { it.type }
        return listOfNotNull(
            currentTerms[TosType.TERMS_OF_SERVICE],
            currentTerms[TosType.PRIVACY_POLICY],
        )
    }
}
