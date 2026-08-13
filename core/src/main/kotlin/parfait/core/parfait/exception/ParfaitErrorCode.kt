package parfait.core.parfait.exception

import parfait.common.error.BaseErrorCode

enum class ParfaitErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    INVALID_DATE_RANGE(400, "INVALID_DATE_RANGE", "조회 시작일이 종료일보다 늦을 수 없습니다"),
}
