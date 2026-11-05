@file:Suppress("ktlint:standard:package-name")

package parfait.core.stats.port.`in`

import java.time.LocalDateTime

interface GetGroupStatsUseCase {
    /** `[from, to)` 구간에 생성된 그룹 수. */
    fun countNewGroups(
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long
}
