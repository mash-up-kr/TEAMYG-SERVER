package parfait.core.report.domain

import java.time.LocalDateTime

data class ReportSettings(
    val adMode: Boolean,
    val intervalDays: Int,
    val lastSentAt: LocalDateTime?,
)
