package parfait.core.parfait.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import parfait.core.parfait.event.ParfaitVersionBumpedEvent
import parfait.core.parfait.port.out.ParfaitVersionPort
import kotlin.test.assertFailsWith

class ParfaitVersionServiceTest {
    private val parfaitVersionPort = mockk<ParfaitVersionPort>(relaxed = true)
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val service = ParfaitVersionService(parfaitVersionPort, eventPublisher)

    @Test
    fun `bump 는 version 을 올린 뒤 무효화 이벤트를 발행한다`() {
        service.bump(5L)

        verifyOrder {
            parfaitVersionPort.bump(5L)
            eventPublisher.publishEvent(ParfaitVersionBumpedEvent(5L))
        }
    }

    @Test
    fun `bump 가 실패하면 이벤트를 발행하지 않는다`() {
        every { parfaitVersionPort.bump(5L) } throws IllegalStateException("boom")

        val exception = assertFailsWith<IllegalStateException> { service.bump(5L) }
        exception.message shouldBe "boom"

        verify(exactly = 0) { eventPublisher.publishEvent(any()) }
    }

    /** 트랜잭션 밖에서 호출하면 AFTER_COMMIT 무효화 이벤트가 조용히 버려지므로 호출 계약을 강제한다. */
    @Test
    fun `bump 는 기존 트랜잭션 안에서만 호출할 수 있다 (MANDATORY)`() {
        val transactional =
            ParfaitVersionService::class.java
                .getMethod("bump", Long::class.javaPrimitiveType)
                .getAnnotation(Transactional::class.java)

        transactional?.propagation shouldBe Propagation.MANDATORY
    }
}
