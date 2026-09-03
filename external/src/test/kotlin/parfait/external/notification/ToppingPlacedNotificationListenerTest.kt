package parfait.external.notification

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.notification.event.ToppingPlacedEvent

class ToppingPlacedNotificationListenerTest {
    private val worker = mockk<OutboxPollingWorker>(relaxed = true)
    private val listener = ToppingPlacedNotificationListener(worker)

    @Test
    fun `이벤트를 받으면 워커를 깨운다`() {
        listener.on(ToppingPlacedEvent(5L))

        verify { worker.wakeUp() }
    }
}
