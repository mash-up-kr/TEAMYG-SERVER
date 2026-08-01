package parfait.core.parfaitgroup.application.port.out

import parfait.core.parfaitgroup.domain.ParfaitGroupReport

interface ParfaitGroupReportSavePort {
    fun save(report: ParfaitGroupReport): ParfaitGroupReport
}
