package parfait.core.member.exception

import parfait.common.error.BaseErrorCode

enum class MemberErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    INVALID_NICKNAME(400, "INVALID_NICKNAME", "닉네임 형식이 올바르지 않습니다"),
    MEMBER_NOT_FOUND(404, "MEMBER_NOT_FOUND", "존재하지 않는 회원입니다"),
}
