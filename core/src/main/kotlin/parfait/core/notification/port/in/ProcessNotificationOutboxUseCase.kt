@file:Suppress("ktlint:standard:package-name")

package parfait.core.notification.port.`in`

import java.time.LocalDateTime

interface ProcessNotificationOutboxUseCase {
    /** 한 번의 클레임 배치 처리. now 는 호출자(external 워커)가 주입. */
    fun processDueBatch(now: LocalDateTime = LocalDateTime.now()): OutboxBatchOutcome
}

data class OutboxBatchOutcome(
    val claimed: Int,
    val sent: Int,
    val cancelled: Int,
    val retried: Int,
    val failed: Int,
)
