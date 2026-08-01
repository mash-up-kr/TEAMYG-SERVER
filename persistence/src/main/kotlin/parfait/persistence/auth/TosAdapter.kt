package parfait.persistence.auth

import org.springframework.stereotype.Component
import parfait.core.auth.port.out.CurrentTerms
import parfait.core.auth.port.out.TosQueryPort
import parfait.persistence.repository.TosRepository

@Component
class TosAdapter(
    private val tosRepository: TosRepository,
) : TosQueryPort {
    override fun findCurrentTerms(): List<CurrentTerms> =
        tosRepository.findCurrentTerms().map { CurrentTerms(id = it.id!!, required = it.required) }
}
