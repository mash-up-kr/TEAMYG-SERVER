package parfait.core.report.domain

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 오늘 리포트를 보내야 하는지, 보낸다면 어떤 기간인지 판단한다.
 * 주기는 시각이 아니라 **날짜 차이**로 비교한다 — 매일 21:00:00 에 도는 스케줄러가 직전 전송(21:00:03)의
 * 3일 뒤에 "2일 23시간 59분"으로 판단해 하루 밀리는 것을 막기 위해서다.
 */
@Component
class ReportPlanner {
    fun plan(
        settings: ReportSettings,
        now: LocalDateTime,
    ): ReportPeriod? {
        val last = settings.lastSentAt
        if (last != null && last.toLocalDate() == now.toLocalDate()) return null

        if (settings.adMode) return ReportPeriod(last ?: now.minusDays(1), now)

        if (last == null) return ReportPeriod(now.minusDays(settings.intervalDays.toLong()), now)
        val elapsedDays = ChronoUnit.DAYS.between(last.toLocalDate(), now.toLocalDate())
        return if (elapsedDays >= settings.intervalDays) ReportPeriod(last, now) else null
    }

    /**
     * 정기 전송이 다음에 일어날 날짜. 오늘 정기 실행 시각이 이미 지났으면 내일부터 센다.
     * 주기가 이미 지났으면 그 기준일(오늘 또는 내일)이다.
     */
    fun nextReportDate(
        settings: ReportSettings,
        now: LocalDateTime,
        sendTime: LocalTime,
    ): LocalDate {
        val base = if (!now.toLocalTime().isBefore(sendTime)) now.toLocalDate().plusDays(1) else now.toLocalDate()
        val last = settings.lastSentAt?.toLocalDate() ?: return base
        val next = last.plusDays(settings.cycleDays())
        return if (next.isBefore(base)) base else next
    }

    /** `/리포트 지금` 용 기간. 정기 전송 여부와 무관하게 현재 모드 기준 직전 구간이다. */
    fun planNow(
        settings: ReportSettings,
        now: LocalDateTime,
    ): ReportPeriod = ReportPeriod(now.minusDays(settings.cycleDays()), now)

    private fun ReportSettings.cycleDays(): Long = if (adMode) 1L else intervalDays.toLong()
}
