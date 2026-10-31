package parfait.core.report.domain

import java.time.LocalDateTime

/** `[from, to)` 반열린 구간. */
data class ReportPeriod(
    val from: LocalDateTime,
    val to: LocalDateTime,
)
