package parfait.common.error

enum class CommonErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    INVALID_REQUEST(400, "INVALID_REQUEST", "요청 형식이 올바르지 않습니다"),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다"),
}
