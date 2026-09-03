package parfait.external.notification

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import parfait.core.notification.event.ToppingPlacedEvent

/** place() 커밋 후 폴러를 즉시 깨운다. 신호가 유실돼도 다음 fixedDelay 폴링이 잡는다. */
@Component
class ToppingPlacedNotificationListener(
    private val worker: OutboxPollingWorker,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ToppingPlacedEvent) {
        worker.wakeUp()
    }
}
