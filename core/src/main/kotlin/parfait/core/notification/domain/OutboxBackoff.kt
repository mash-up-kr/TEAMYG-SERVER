package parfait.core.notification.domain

import java.time.Duration

object OutboxBackoff {
    const val MAX_ATTEMPTS = 5

    private val SCHEDULE =
        listOf(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofHours(1),
            Duration.ofHours(6),
        )

    /** attempts: 이번 실패까지 누적 시도 횟수(1-based). 스케줄을 넘으면 마지막 간격. */
    fun nextDelay(attempts: Int): Duration = SCHEDULE.getOrElse(attempts - 1) { SCHEDULE.last() }
}
