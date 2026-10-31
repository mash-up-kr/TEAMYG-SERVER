package parfait.core.stats.domain

data class SignupStats(
    val newSignups: Long,
    val withdrawals: Long,
    val newSignupWithdrawals: Long,
    val totalUsers: Long,
) {
    val netGrowth: Long get() = newSignups - withdrawals
}
