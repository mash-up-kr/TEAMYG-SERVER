package parfait.core.report.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class ReportStatus(
    val adMode: Boolean,
    val intervalDays: Int,
    val lastSentAt: LocalDateTime?,
    val nextReportDate: LocalDate,
)
