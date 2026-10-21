package parfait.core.parfaittext.exception

import parfait.common.error.BaseErrorCode

enum class ParfaitTextErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    INVALID_TEXT(400, "INVALID_TEXT", "텍스트는 공백이 아닌 1~2000자여야 합니다"),
    INVALID_TEXT_STYLE(400, "INVALID_TEXT_STYLE", "색상, 위치, 각도, 확대 배율 또는 폰트 크기가 올바르지 않습니다"),
}
