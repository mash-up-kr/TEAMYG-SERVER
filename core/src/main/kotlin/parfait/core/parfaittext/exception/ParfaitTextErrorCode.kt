package parfait.core.parfaittext.exception

import parfait.common.error.BaseErrorCode

enum class ParfaitTextErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    PARFAIT_TEXT_NOT_FOUND(404, "PARFAIT_TEXT_NOT_FOUND", "존재하지 않는 텍스트 배치입니다"),
    PARFAIT_TEXT_NOT_OWNED(403, "PARFAIT_TEXT_NOT_OWNED", "본인이 배치한 텍스트가 아닙니다"),
    INVALID_TEXT(400, "INVALID_TEXT", "텍스트는 공백이 아닌 1~2000자여야 합니다"),
    INVALID_TEXT_STYLE(400, "INVALID_TEXT_STYLE", "색상, 위치, 각도, 확대 배율 또는 폰트 크기가 올바르지 않습니다"),
}
