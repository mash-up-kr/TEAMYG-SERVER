@file:Suppress("ktlint:standard:package-name")

package parfait.core.stats.port.`in`

import parfait.core.stats.domain.SignupStatsComparison
import java.time.LocalDateTime

interface GetSignupStatsComparisonUseCase {
    /** `[from, to)` 통계와 직전 같은 길이 기간 `[from - (to - from), from)` 통계를 함께 돌려준다. */
    fun getComparison(
        from: LocalDateTime,
        to: LocalDateTime,
    ): SignupStatsComparison
}
