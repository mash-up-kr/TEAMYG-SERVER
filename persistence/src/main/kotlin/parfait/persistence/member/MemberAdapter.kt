package parfait.persistence.member

import org.springframework.stereotype.Component
import parfait.core.exception.BusinessException
import parfait.core.member.exception.MemberErrorCode
import parfait.core.member.port.out.MemberAccount
import parfait.core.member.port.out.MemberCreatePort
import parfait.core.member.port.out.MemberDeletePort
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
    MemberNicknameUpdatePort,
    MemberDeletePort {
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

    override fun findAccountById(memberId: Long): MemberAccount? =
        memberRepository.findById(memberId).orElse(null)?.let {
            MemberAccount(provider = it.loginProvider.toCoreProvider(), nickname = it.globalNickname)
        }

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

    override fun deleteById(memberId: Long) {
        val member =
            memberRepository.findById(memberId).orElseThrow {
                BusinessException(MemberErrorCode.MEMBER_NOT_FOUND)
            }
        member.providerUserId = "withdrawn_$memberId"
        memberRepository.save(member)
        // save()와 delete()가 같은 트랜잭션(같은 flush)에 걸리면, 같은 엔티티가 곧이어 제거될
        // 예정이라는 이유로 Hibernate가 이 UPDATE(rename)를 dirty-check에서 건너뛰고 DB에
        // 반영하지 않는다 — provider_user_id가 원래 값 그대로 남아 재가입 시 유니크 제약
        // 위반으로 이어진다. flush()로 rename을 먼저 DB에 강제 반영한 뒤 delete를 건다.
        memberRepository.flush()
        memberRepository.delete(member)
    }

    private fun CoreLoginProvider.toPersistenceProvider(): LoginProvider =
        when (this) {
            CoreLoginProvider.KAKAO -> LoginProvider.KAKAO
            CoreLoginProvider.APPLE -> LoginProvider.APPLE
        }

    private fun LoginProvider.toCoreProvider(): CoreLoginProvider =
        when (this) {
            LoginProvider.KAKAO -> CoreLoginProvider.KAKAO
            LoginProvider.APPLE -> CoreLoginProvider.APPLE
            LoginProvider.GOOGLE -> error("GOOGLE login provider is not supported yet")
        }
}
