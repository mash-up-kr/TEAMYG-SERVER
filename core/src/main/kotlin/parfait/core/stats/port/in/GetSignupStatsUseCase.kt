@file:Suppress("ktlint:standard:package-name")

package parfait.core.stats.port.`in`

import parfait.core.stats.domain.SignupStats
import java.time.LocalDateTime

interface GetSignupStatsUseCase {
    /** 기간은 `[from, to)` 반열린 구간이다. */
    fun getSignupStats(
        from: LocalDateTime,
        to: LocalDateTime,
    ): SignupStats
}
