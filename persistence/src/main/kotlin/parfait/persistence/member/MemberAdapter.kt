package parfait.persistence.member

import org.springframework.stereotype.Component
import parfait.core.member.port.out.MemberCreatePort
import parfait.core.member.port.out.MemberQueryPort
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.repository.MemberRepository
import parfait.core.auth.domain.LoginProvider as CoreLoginProvider

@Component
class MemberAdapter(
    private val memberRepository: MemberRepository,
) : MemberQueryPort,
    MemberCreatePort {
    override fun existsById(memberId: Long): Boolean = memberRepository.existsById(memberId)

    override fun findMemberIdByProvider(
        provider: CoreLoginProvider,
        providerUserId: String,
    ): Long? =
        memberRepository
            .findByLoginProviderAndProviderUserId(provider.toPersistenceProvider(), providerUserId)
            ?.id

    override fun create(
        provider: CoreLoginProvider,
        providerUserId: String,
        nickname: String,
    ): Long =
        memberRepository
            .save(
                Member(
                    loginProvider = provider.toPersistenceProvider(),
                    providerUserId = providerUserId,
                    globalNickname = nickname,
                ),
            ).id!!

    private fun CoreLoginProvider.toPersistenceProvider(): LoginProvider =
        when (this) {
            CoreLoginProvider.KAKAO -> LoginProvider.KAKAO
            CoreLoginProvider.APPLE -> LoginProvider.APPLE
        }
}
