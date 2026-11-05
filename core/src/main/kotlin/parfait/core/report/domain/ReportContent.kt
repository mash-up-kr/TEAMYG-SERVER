package parfait.core.report.domain

import parfait.core.stats.domain.DailySignupStats
import parfait.core.stats.domain.SignupGrowth
import parfait.core.stats.domain.SignupStats

data class ReportContent(
    val period: ReportPeriod,
    val signupStats: SignupStats,
    val growth: SignupGrowth? = null,
    /** 신규 그룹 수. `null`이면 요약에 표시하지 않는다. */
    val newGroups: Long? = null,
    /** 달력 날짜별 상세. 비어 있으면 일별 필드를 만들지 않는다. */
    val daily: List<DailySignupStats> = emptyList(),
)
