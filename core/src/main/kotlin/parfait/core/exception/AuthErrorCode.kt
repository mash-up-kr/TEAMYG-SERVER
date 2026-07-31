package parfait.core.exception

import parfait.common.error.BaseErrorCode

enum class AuthErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다"),
    INVALID_TOKEN(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(401, "EXPIRED_TOKEN", "만료된 토큰입니다"),
    MEMBER_NOT_FOUND(401, "MEMBER_NOT_FOUND", "존재하지 않는 회원입니다"),
}
