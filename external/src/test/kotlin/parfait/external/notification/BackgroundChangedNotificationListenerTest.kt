package parfait.external.notification

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.notification.event.BackgroundChangedEvent

class BackgroundChangedNotificationListenerTest {
    private val worker = mockk<OutboxPollingWorker>(relaxed = true)
    private val listener = BackgroundChangedNotificationListener(worker)

    @Test
    fun `이벤트를 받으면 워커를 깨운다`() {
        listener.on(BackgroundChangedEvent(5L))

        verify { worker.wakeUp() }
    }
}
