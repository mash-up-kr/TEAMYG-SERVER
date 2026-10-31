@file:Suppress("ktlint:standard:package-name")

package parfait.core.report.port.`in`

import parfait.core.report.domain.ReportStatus
import java.time.LocalDateTime

interface ChangeAdModeUseCase {
    fun changeAdMode(
        adMode: Boolean,
        now: LocalDateTime,
    ): ReportStatus
}
