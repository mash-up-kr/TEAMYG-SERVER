package parfait.core.report.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import parfait.core.report.domain.ReportPlanner
import parfait.core.report.domain.ReportSettings
import parfait.core.report.domain.ReportStatus
import parfait.core.report.port.`in`.ChangeAdModeUseCase
import parfait.core.report.port.`in`.GetReportStatusUseCase
import parfait.core.report.port.out.ReportSettingsQueryPort
import parfait.core.report.port.out.ReportSettingsSavePort
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class ReportSettingsService(
    private val settingsQueryPort: ReportSettingsQueryPort,
    private val settingsSavePort: ReportSettingsSavePort,
    private val planner: ReportPlanner,
    // report.hour(0~23)를 정시 LocalTime으로 바꾼다. 범위를 벗어나면 기동 시 예외가 난다.
    @Value("#{T(java.time.LocalTime).of(\${report.hour:21}, 0)}") private val sendTime: LocalTime,
) : ChangeAdModeUseCase,
    GetReportStatusUseCase {
    override fun getStatus(now: LocalDateTime): ReportStatus = toStatus(settingsQueryPort.find(), now)

    override fun changeAdMode(
        adMode: Boolean,
        now: LocalDateTime,
    ): ReportStatus {
        settingsSavePort.saveAdMode(adMode)
        return toStatus(settingsQueryPort.find(), now)
    }

    private fun toStatus(
        settings: ReportSettings,
        now: LocalDateTime,
    ) = ReportStatus(
        adMode = settings.adMode,
        intervalDays = settings.intervalDays,
        lastSentAt = settings.lastSentAt,
        nextReportDate = planner.nextReportDate(settings, now, sendTime),
    )
}
