package parfait.core.stats.domain

/** 직전 같은 길이 기간 대비 증감률(%). 직전 값이 0이라 계산할 수 없으면 `null`이다. */
data class SignupGrowth(
    val newSignupsChangePercent: Double?,
    val totalUsersGrowthPercent: Double?,
) {
    companion object {
        fun between(
            current: SignupStats,
            previous: SignupStats,
        ): SignupGrowth =
            SignupGrowth(
                newSignupsChangePercent = percentChange(current.newSignups, previous.newSignups),
                totalUsersGrowthPercent = percentChange(current.totalUsers, previous.totalUsers),
            )

        private fun percentChange(
            current: Long,
            previous: Long,
        ): Double? = if (previous == 0L) null else (current - previous) * 100.0 / previous
    }
}
