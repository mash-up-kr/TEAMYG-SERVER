package parfait.core.auth.exception

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
    INVALID_ID_TOKEN(401, "INVALID_ID_TOKEN", "유효하지 않은 ID 토큰입니다"),
    KAKAO_JWKS_FETCH_FAILED(502, "KAKAO_JWKS_FETCH_FAILED", "카카오 공개키 조회에 실패했습니다"),
    KAKAO_SERVER_UNAVAILABLE(503, "KAKAO_SERVER_UNAVAILABLE", "카카오 서버에 연결할 수 없습니다"),
    ALREADY_REGISTERED(409, "ALREADY_REGISTERED", "이미 가입된 회원입니다"),
    DUPLICATE_TERMS_ID(400, "DUPLICATE_TERMS_ID", "중복된 약관 ID입니다"),
    TERMS_NOT_FOUND(400, "TERMS_NOT_FOUND", "존재하지 않는 약관입니다"),
    REQUIRED_TERMS_NOT_AGREED(400, "REQUIRED_TERMS_NOT_AGREED", "필수 약관에 모두 동의해야 합니다"),
    FORBIDDEN_REFRESH_TOKEN(403, "FORBIDDEN_REFRESH_TOKEN", "다른 회원의 Refresh Token입니다"),
    APPLE_SERVER_ERROR(502, "APPLE_SERVER_ERROR", "애플 서버 응답 오류입니다"),
    APPLE_SERVER_UNAVAILABLE(503, "APPLE_SERVER_UNAVAILABLE", "애플 서버에 연결할 수 없습니다"),
}
