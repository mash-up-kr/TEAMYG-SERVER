package parfait.persistence.auth

import org.springframework.stereotype.Component
import parfait.core.auth.port.out.CurrentTerms
import parfait.core.auth.port.out.TosQueryPort
import parfait.persistence.entity.TosType
import parfait.persistence.repository.TosRepository
import parfait.core.auth.domain.TosType as CoreTosType

@Component
class TosAdapter(
    private val tosRepository: TosRepository,
) : TosQueryPort {
    override fun findCurrentTerms(): List<CurrentTerms> =
        tosRepository.findCurrentTerms().map {
            CurrentTerms(
                id = it.id!!,
                type = it.type.toCoreType(),
                title = it.title,
                url = it.content,
                required = it.required,
            )
        }

    private fun TosType.toCoreType(): CoreTosType =
        when (this) {
            TosType.TERMS_OF_SERVICE -> CoreTosType.TERMS_OF_SERVICE
            TosType.PRIVACY_POLICY -> CoreTosType.PRIVACY_POLICY
        }
}
