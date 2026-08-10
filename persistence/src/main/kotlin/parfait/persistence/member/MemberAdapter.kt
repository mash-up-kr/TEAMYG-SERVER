package parfait.persistence.member

import org.springframework.stereotype.Component
import parfait.core.exception.BusinessException
import parfait.core.member.exception.MemberErrorCode
import parfait.core.member.port.out.MemberAppleRefreshTokenSavePort
import parfait.core.member.port.out.MemberCreatePort
import parfait.core.member.port.out.MemberNicknameUpdatePort
import parfait.core.member.port.out.MemberQueryPort
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.repository.MemberRepository
import parfait.core.auth.domain.LoginProvider as CoreLoginProvider

@Component
class MemberAdapter(
    private val memberRepository: MemberRepository,
) : MemberQueryPort,
    MemberCreatePort,
    MemberAppleRefreshTokenSavePort,
    MemberNicknameUpdatePort {
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

    override fun saveRefreshToken(
        memberId: Long,
        appleRefreshToken: String,
    ) {
        val member = memberRepository.findById(memberId).get()
        member.appleRefreshToken = appleRefreshToken
        memberRepository.save(member)
    }

    override fun updateGlobalNickname(
        memberId: Long,
        nickname: String,
    ) {
        val member =
            memberRepository.findById(memberId).orElseThrow {
                BusinessException(MemberErrorCode.MEMBER_NOT_FOUND)
            }
        member.globalNickname = nickname
        memberRepository.save(member)
    }

    private fun CoreLoginProvider.toPersistenceProvider(): LoginProvider =
        when (this) {
            CoreLoginProvider.KAKAO -> LoginProvider.KAKAO
            CoreLoginProvider.APPLE -> LoginProvider.APPLE
        }
}
