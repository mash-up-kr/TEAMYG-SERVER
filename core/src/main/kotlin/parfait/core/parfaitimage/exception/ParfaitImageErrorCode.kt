package parfait.core.parfaitimage.exception

import parfait.common.error.BaseErrorCode

enum class ParfaitImageErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    PARFAIT_NOT_FOUND(404, "PARFAIT_NOT_FOUND", "존재하지 않는 파르페입니다"),
    IMAGE_NOT_CONFIRMED(409, "IMAGE_NOT_CONFIRMED", "업로드가 확인되지 않은 이미지입니다"),
    INVALID_BORDER(400, "INVALID_BORDER", "SOLID 테두리는 색상과 두께가 필요합니다"),
    PARFAIT_IMAGE_NOT_FOUND(404, "PARFAIT_IMAGE_NOT_FOUND", "존재하지 않는 배치입니다"),
    PARFAIT_IMAGE_NOT_OWNED(403, "PARFAIT_IMAGE_NOT_OWNED", "본인이 배치한 토핑이 아닙니다"),
}
