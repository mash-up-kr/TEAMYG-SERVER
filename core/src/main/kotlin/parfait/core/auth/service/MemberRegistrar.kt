package parfait.core.auth.service

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.port.out.TosAgreementSavePort
import parfait.core.member.port.out.MemberCreatePort

@Component
class MemberRegistrar(
    private val memberCreatePort: MemberCreatePort,
    private val tosAgreementSavePort: TosAgreementSavePort,
) {
    @Transactional
    fun register(
        provider: LoginProvider,
        providerUserId: String,
        nickname: String,
        agreedTosIds: List<Long>,
    ): Long {
        val memberId = memberCreatePort.create(provider, providerUserId, nickname)
        tosAgreementSavePort.saveAll(memberId, agreedTosIds)
        return memberId
    }
}
