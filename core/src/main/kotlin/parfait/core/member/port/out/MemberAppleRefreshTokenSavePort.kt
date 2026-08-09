package parfait.core.member.port.out

interface MemberAppleRefreshTokenSavePort {
    fun saveRefreshToken(
        memberId: Long,
        appleRefreshToken: String,
    )
}
