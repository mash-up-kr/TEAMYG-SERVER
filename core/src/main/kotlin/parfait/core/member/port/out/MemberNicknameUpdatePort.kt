package parfait.core.member.port.out

interface MemberNicknameUpdatePort {
    fun updateGlobalNickname(
        memberId: Long,
        nickname: String,
    )
}
