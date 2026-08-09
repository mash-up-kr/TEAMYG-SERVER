package parfait.core.member.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.member.domain.GlobalNickname
import parfait.core.member.port.`in`.ChangeGlobalNicknameResult
import parfait.core.member.port.`in`.ChangeGlobalNicknameUseCase
import parfait.core.member.port.out.MemberNicknameUpdatePort

@Service
class MemberService(
    private val memberNicknameUpdatePort: MemberNicknameUpdatePort,
) : ChangeGlobalNicknameUseCase {
    @Transactional
    override fun change(
        memberId: Long,
        nickname: String,
    ): ChangeGlobalNicknameResult {
        val globalNickname = GlobalNickname.of(nickname)
        memberNicknameUpdatePort.updateGlobalNickname(memberId, globalNickname.value)
        return ChangeGlobalNicknameResult(globalNickname.value)
    }
}
