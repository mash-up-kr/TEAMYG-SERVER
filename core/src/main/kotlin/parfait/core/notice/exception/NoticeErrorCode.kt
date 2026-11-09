package parfait.core.notice.exception

import parfait.common.error.BaseErrorCode

enum class NoticeErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    INVALID_NOTICE_TOKEN(401, "INVALID_NOTICE_TOKEN", "공지사항 어드민 토큰이 올바르지 않습니다"),
    INVALID_NOTICE(400, "INVALID_NOTICE", "공지사항 내용이 올바르지 않습니다"),
    INVALID_NOTICE_PLATFORM(400, "INVALID_NOTICE_PLATFORM", "플랫폼 값이 올바르지 않습니다 (IOS 또는 ANDROID만 가능합니다)"),
    NOTICE_NOT_FOUND(404, "NOTICE_NOT_FOUND", "공지사항을 찾을 수 없습니다"),
}
