package parfait.http.api.stats.dto

import parfait.core.stats.domain.SignupStats

data class SignupStatsResponse(
    val newSignups: Long,
    val withdrawals: Long,
    val newSignupWithdrawals: Long,
    val netGrowth: Long,
    val totalUsers: Long,
) {
    companion object {
        fun from(stats: SignupStats) =
            SignupStatsResponse(
                newSignups = stats.newSignups,
                withdrawals = stats.withdrawals,
                newSignupWithdrawals = stats.newSignupWithdrawals,
                netGrowth = stats.netGrowth,
                totalUsers = stats.totalUsers,
            )
    }
}
