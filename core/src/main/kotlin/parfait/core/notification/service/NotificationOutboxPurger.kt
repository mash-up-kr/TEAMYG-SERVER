package parfait.core.notification.service

import org.springframework.stereotype.Service
import parfait.core.notification.port.`in`.PurgeNotificationOutboxUseCase
import parfait.core.notification.port.out.NotificationOutboxPurgePort
import java.time.Duration
import java.time.LocalDateTime

@Service
class NotificationOutboxPurger(
    private val purgePort: NotificationOutboxPurgePort,
) : PurgeNotificationOutboxUseCase {
    override fun purgeTerminal(
        retention: Duration,
        now: LocalDateTime,
    ): Int = purgePort.deleteTerminalBefore(now.minus(retention))
}
