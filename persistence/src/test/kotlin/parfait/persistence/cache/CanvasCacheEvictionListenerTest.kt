package parfait.persistence.cache

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.parfait.event.ParfaitVersionBumpedEvent
import parfait.core.parfait.port.out.CanvasCachePort
import parfait.core.parfaitgroup.application.event.GroupMemberLeftEvent
import parfait.core.parfaitgroup.application.event.GroupVersionBumpedEvent

class CanvasCacheEvictionListenerTest {
    private val cache = mockk<CanvasCachePort>(relaxed = true)
    private val listener = CanvasCacheEvictionListener(cache)

    @Test
    fun `캔버스 version 이벤트는 parfait version 키를 지운다`() {
        listener.on(ParfaitVersionBumpedEvent(5L))

        verify(exactly = 1) { cache.evictParfaitVersion(5L) }
    }

    @Test
    fun `그룹 version 이벤트는 group version 키를 지운다`() {
        listener.on(GroupVersionBumpedEvent(1L))

        verify(exactly = 1) { cache.evictGroupVersion(1L) }
    }

    @Test
    fun `탈퇴 이벤트는 해당 멤버의 멤버 캐시 키를 지운다`() {
        listener.on(GroupMemberLeftEvent(1L, 42L))

        verify(exactly = 1) { cache.evictMember(1L, 42L) }
    }
}
