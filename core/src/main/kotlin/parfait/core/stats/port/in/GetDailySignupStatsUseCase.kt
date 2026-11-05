@file:Suppress("ktlint:standard:package-name")

package parfait.core.stats.port.`in`

import parfait.core.stats.domain.DailySignupStats
import java.time.LocalDateTime

interface GetDailySignupStatsUseCase {
    /**
     * `[from, to)` 구간을 달력 날짜별로 집계한다.
     * 가입·탈퇴가 없는 날도 0으로 채워 `from`의 날짜부터 `to` 직전 시각의 날짜까지 모두 돌려준다.
     */
    fun getDailySignupStats(
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<DailySignupStats>
}
