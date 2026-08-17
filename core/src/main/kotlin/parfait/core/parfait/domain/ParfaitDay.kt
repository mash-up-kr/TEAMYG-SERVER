package parfait.core.parfait.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// 파르페의 "하루"는 자정이 아니라 새벽 3시에 넘어간다(ParfaitCanvasRotationScheduler의 배치
// 실행 시각과 동일한 기준). 자정~새벽 3시 사이에는 아직 전날 캔버스가 진행 중인 것으로 본다 —
// 이 기준 없이 LocalDate.now()를 그대로 쓰면, 그 시간대에 그룹을 만들거나 오늘의 캔버스를
// 조회할 때 캔버스가 하루 이른 날짜로 생성돼버린다.
object ParfaitDay {
    private val ROLLOVER_TIME: LocalTime = LocalTime.of(3, 0)

    fun current(now: LocalDateTime = LocalDateTime.now()): LocalDate =
        if (now.toLocalTime().isBefore(ROLLOVER_TIME)) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }
}
