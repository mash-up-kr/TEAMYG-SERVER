@file:Suppress("ktlint:standard:package-name")

package parfait.core.report.port.`in`

import parfait.core.report.domain.ReportSendOutcome
import java.time.LocalDateTime

interface SendScheduledReportUseCase {
    fun sendIfDue(now: LocalDateTime): ReportSendOutcome
}
