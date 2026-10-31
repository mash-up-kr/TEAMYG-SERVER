package parfait.core.stats.exception

import parfait.common.error.BaseErrorCode

enum class StatsErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    INVALID_STATS_TOKEN(401, "INVALID_STATS_TOKEN", "통계 API 토큰이 올바르지 않습니다"),
    INVALID_STATS_PERIOD(400, "INVALID_STATS_PERIOD", "조회 기간이 올바르지 않습니다 (시작은 끝보다 앞이어야 하고 최대 366일까지 조회할 수 있습니다)"),
}
