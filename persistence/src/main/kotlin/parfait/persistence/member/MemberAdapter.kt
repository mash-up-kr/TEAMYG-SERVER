package parfait.persistence.member

import org.springframework.stereotype.Component
import parfait.core.member.port.out.MemberQueryPort
import parfait.persistence.entity.LoginProvider
import parfait.persistence.repository.MemberRepository
import parfait.core.auth.domain.LoginProvider as CoreLoginProvider

@Component
class MemberAdapter(
    private val memberRepository: MemberRepository,
) : MemberQueryPort {
    override fun existsById(memberId: Long): Boolean = memberRepository.existsById(memberId)

    override fun findGlobalNicknameById(memberId: Long): String? =
        memberRepository.findById(memberId).orElse(null)?.globalNickname

    override fun findMemberIdByProvider(
        provider: CoreLoginProvider,
        providerUserId: String,
    ): Long? =
        memberRepository
            .findByLoginProviderAndProviderUserId(provider.toPersistenceProvider(), providerUserId)
            ?.id

    private fun CoreLoginProvider.toPersistenceProvider(): LoginProvider =
        when (this) {
            CoreLoginProvider.KAKAO -> LoginProvider.KAKAO
            CoreLoginProvider.APPLE -> LoginProvider.APPLE
        }
}
