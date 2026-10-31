package parfait.core.report.port.out

import parfait.core.report.domain.ReportSettings

interface ReportSettingsQueryPort {
    fun find(): ReportSettings
}
