package parfait.core.member.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.member.domain.GlobalNickname
import parfait.core.member.exception.MemberErrorCode
import parfait.core.member.port.`in`.ChangeGlobalNicknameResult
import parfait.core.member.port.`in`.ChangeGlobalNicknameUseCase
import parfait.core.member.port.`in`.GetMyAccountUseCase
import parfait.core.member.port.`in`.MyAccountResult
import parfait.core.member.port.out.MemberNicknameUpdatePort
import parfait.core.member.port.out.MemberQueryPort

@Service
class MemberService(
    private val memberNicknameUpdatePort: MemberNicknameUpdatePort,
    private val memberQueryPort: MemberQueryPort,
) : ChangeGlobalNicknameUseCase,
    GetMyAccountUseCase {
    @Transactional
    override fun change(
        memberId: Long,
        nickname: String,
    ): ChangeGlobalNicknameResult {
        val globalNickname = GlobalNickname.of(nickname)
        memberNicknameUpdatePort.updateGlobalNickname(memberId, globalNickname.value)
        return ChangeGlobalNicknameResult(globalNickname.value)
    }

    override fun getMyAccount(memberId: Long): MyAccountResult {
        val account =
            memberQueryPort.findAccountById(memberId)
                ?: throw BusinessException(MemberErrorCode.MEMBER_NOT_FOUND)
        return MyAccountResult(memberId, account.provider, account.nickname)
    }
}
