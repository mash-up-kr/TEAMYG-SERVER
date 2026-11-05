package parfait.core.stats.port.out

import parfait.core.stats.domain.DailySignupStats
import parfait.core.stats.domain.SignupStats
import java.time.LocalDateTime

interface SignupStatsQueryPort {
    fun aggregate(
        from: LocalDateTime,
        to: LocalDateTime,
    ): SignupStats

    /** `[from, to)` 구간에서 가입·탈퇴가 있었던 날짜별 집계. 활동이 없는 날은 포함하지 않는다. */
    fun aggregateDaily(
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<DailySignupStats>
}
