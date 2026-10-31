package parfait.core.stats.domain

data class SignupStatsComparison(
    val current: SignupStats,
    /** 직전 기간 집계에 실패하면 `null`이다. */
    val previous: SignupStats?,
) {
    val growth: SignupGrowth? get() = previous?.let { SignupGrowth.between(current, it) }
}
