package parfait.core.stats.port.out

import parfait.core.stats.domain.SignupStats
import java.time.LocalDateTime

interface SignupStatsQueryPort {
    fun aggregate(
        from: LocalDateTime,
        to: LocalDateTime,
    ): SignupStats
}
