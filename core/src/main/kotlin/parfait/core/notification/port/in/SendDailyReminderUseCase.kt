@file:Suppress("ktlint:standard:package-name")

package parfait.core.notification.port.`in`

import parfait.core.notification.domain.ReminderType
import java.time.LocalDateTime

interface SendDailyReminderUseCase {
    /** now 는 배치 시작 시각. P-03 의 21:00 하드컷 판정과 로깅에 쓰인다. */
    fun send(
        type: ReminderType,
        now: LocalDateTime = LocalDateTime.now(),
    ): ReminderSendOutcome
}

data class ReminderSendOutcome(
    val skipped: Boolean, // P-03 & 배치 시작 시각 >= 21:00
    val targeted: Int,
    val sent: Int,
    val failed: Int,
    val deadTokensDeleted: Int,
)
