package parfait.core.parfaitgroup.application.service

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import parfait.core.parfaitgroup.application.event.GroupMemberLeftEvent
import parfait.core.parfaitgroup.application.event.GroupVersionBumpedEvent
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupVersionPort

class ParfaitGroupVersionServiceTest {
    private val groupVersionPort = mockk<ParfaitGroupVersionPort>(relaxed = true)
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val service = ParfaitGroupVersionService(groupVersionPort, eventPublisher)

    @Test
    fun `bump 는 version 을 올린 뒤 무효화 이벤트를 발행한다`() {
        service.bump(1L)

        verifyOrder {
            groupVersionPort.bump(1L)
            eventPublisher.publishEvent(GroupVersionBumpedEvent(1L))
        }
    }

    @Test
    fun `bumpOnMemberLeft 는 version 을 올리고 버전·탈퇴 이벤트를 모두 발행한다`() {
        service.bumpOnMemberLeft(groupId = 1L, memberId = 42L)

        verify { groupVersionPort.bump(1L) }
        verify { eventPublisher.publishEvent(GroupVersionBumpedEvent(1L)) }
        verify { eventPublisher.publishEvent(GroupMemberLeftEvent(1L, 42L)) }
    }

    /** 트랜잭션 밖에서 호출하면 AFTER_COMMIT 무효화 이벤트가 조용히 버려지므로 호출 계약을 강제한다. */
    @Test
    fun `bump 와 bumpOnMemberLeft 는 기존 트랜잭션 안에서만 호출할 수 있다 (MANDATORY)`() {
        val type = ParfaitGroupVersionService::class.java
        val long = Long::class.javaPrimitiveType

        type.getMethod("bump", long).getAnnotation(Transactional::class.java)?.propagation shouldBe
            Propagation.MANDATORY
        type.getMethod("bumpOnMemberLeft", long, long).getAnnotation(Transactional::class.java)?.propagation shouldBe
            Propagation.MANDATORY
    }
}
