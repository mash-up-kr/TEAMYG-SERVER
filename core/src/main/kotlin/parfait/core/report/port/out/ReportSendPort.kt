package parfait.core.report.port.out

import parfait.core.report.domain.ReportContent

interface ReportSendPort {
    /** 전송에 실패하면 예외를 던진다. */
    fun send(content: ReportContent)
}
