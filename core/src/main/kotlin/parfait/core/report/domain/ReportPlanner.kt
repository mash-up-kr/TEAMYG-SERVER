package parfait.core.report.domain

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 오늘 리포트를 보내야 하는지, 보낸다면 어떤 기간인지 판단한다.
 * 정기 리포트는 오전에 전날까지의 달력 날짜를 보낸다. 구간은 `[시작일 0시, 오늘 0시)`라서 오늘은 포함하지 않는다.
 * 주기는 시각이 아니라 **날짜 차이**로 비교한다 — 매일 07:00:00 에 도는 스케줄러가 직전 전송(07:00:03)의
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

        val today = now.toLocalDate().atStartOfDay()
        // 마지막 전송이 오늘 0시 직전까지를 보냈으므로 다음 구간은 그 전송일 0시부터 이어진다.
        val lastDay = last?.toLocalDate()?.atStartOfDay()

        if (settings.adMode) return ReportPeriod(lastDay ?: today.minusDays(1), today)

        if (lastDay == null) return ReportPeriod(today.minusDays(settings.intervalDays.toLong()), today)
        val elapsedDays = ChronoUnit.DAYS.between(lastDay.toLocalDate(), now.toLocalDate())
        return if (elapsedDays >= settings.intervalDays) ReportPeriod(lastDay, today) else null
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

    private fun ReportSettings.cycleDays(): Long = if (adMode) 1L else intervalDays.toLong()
}
