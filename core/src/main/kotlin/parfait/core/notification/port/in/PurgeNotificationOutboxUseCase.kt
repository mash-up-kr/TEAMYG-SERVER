@file:Suppress("ktlint:standard:package-name")

package parfait.core.notification.port.`in`

import java.time.Duration
import java.time.LocalDateTime

interface PurgeNotificationOutboxUseCase {
    fun purgeTerminal(
        retention: Duration,
        now: LocalDateTime = LocalDateTime.now(),
    ): Int
}
