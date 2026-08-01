package parfait.persistence.adapter

import org.springframework.stereotype.Component
import parfait.core.port.out.MemberQueryPort
import parfait.persistence.repository.MemberRepository

@Component
class MemberAdapter(
    private val memberRepository: MemberRepository,
) : MemberQueryPort {
    override fun existsById(memberId: Long): Boolean = memberRepository.existsById(memberId)

    override fun findGlobalNicknameById(memberId: Long): String? =
        memberRepository.findById(memberId).orElse(null)?.globalNickname
}
