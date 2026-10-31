@file:Suppress("ktlint:standard:package-name")

package parfait.core.report.port.`in`

import parfait.core.report.domain.ReportContent
import java.time.LocalDateTime

interface BuildReportNowUseCase {
    /** 정기 전송과 무관하게 지금 시점의 리포트 내용을 만든다. 전송·`last_sent_at` 갱신은 하지 않는다. */
    fun buildNow(now: LocalDateTime): ReportContent
}
