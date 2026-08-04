package parfait.core.image.exception

import parfait.common.error.BaseErrorCode

enum class ImageErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    INVALID_CONTENT_TYPE(400, "INVALID_CONTENT_TYPE", "지원하지 않는 이미지 형식입니다"),
    MEMBER_NOT_FOUND(404, "MEMBER_NOT_FOUND", "존재하지 않는 회원입니다"),
    IMAGE_NOT_FOUND(404, "IMAGE_NOT_FOUND", "존재하지 않는 이미지입니다"),
    IMAGE_ALREADY_CONFIRMED(409, "IMAGE_ALREADY_CONFIRMED", "이미 확인된 이미지입니다"),
}
