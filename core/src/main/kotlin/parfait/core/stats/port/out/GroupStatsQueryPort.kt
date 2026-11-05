package parfait.core.stats.port.out

import java.time.LocalDateTime

interface GroupStatsQueryPort {
    /** `[from, to)` 구간에 생성된 그룹 수. */
    fun countCreated(
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long
}
