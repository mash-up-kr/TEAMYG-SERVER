package parfait.persistence.report

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.report.domain.ReportSettings
import parfait.core.report.port.out.ReportSettingsQueryPort
import parfait.core.report.port.out.ReportSettingsSavePort
import parfait.persistence.entity.ReportSetting
import parfait.persistence.repository.ReportSettingRepository
import java.time.LocalDateTime

@Component
class ReportSettingsAdapter(
    private val reportSettingRepository: ReportSettingRepository,
) : ReportSettingsQueryPort,
    ReportSettingsSavePort {
    override fun find(): ReportSettings {
        val setting = loadSetting()
        return ReportSettings(
            adMode = setting.adMode,
            intervalDays = setting.intervalDays,
            lastSentAt = setting.lastSentAt,
        )
    }

    @Transactional
    override fun saveLastSentAt(sentAt: LocalDateTime) {
        val setting = loadSetting()
        setting.lastSentAt = sentAt
        setting.updatedAt = LocalDateTime.now()
        reportSettingRepository.save(setting)
    }

    @Transactional
    override fun saveAdMode(adMode: Boolean) {
        val setting = loadSetting()
        setting.adMode = adMode
        setting.updatedAt = LocalDateTime.now()
        reportSettingRepository.save(setting)
    }

    private fun loadSetting(): ReportSetting =
        reportSettingRepository
            .findById(ReportSetting.SETTINGS_ID)
            .orElseThrow { IllegalStateException("report_settings 기본 행(id=1)이 없습니다 — V24 마이그레이션을 확인하세요") }
}
