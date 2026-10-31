package parfait.core.report.port.out

import java.time.LocalDateTime

interface ReportSettingsSavePort {
    fun saveLastSentAt(sentAt: LocalDateTime)

    fun saveAdMode(adMode: Boolean)
}
