package parfait.core.report.domain

import parfait.core.stats.domain.SignupGrowth
import parfait.core.stats.domain.SignupStats

data class ReportContent(
    val period: ReportPeriod,
    val signupStats: SignupStats,
    val growth: SignupGrowth? = null,
)
