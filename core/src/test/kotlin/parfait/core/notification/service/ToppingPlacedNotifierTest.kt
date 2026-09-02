package parfait.core.notification.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import parfait.core.notification.domain.NotificationOutbox
import parfait.core.notification.domain.ToppingPlacedPayload
import parfait.core.notification.event.ToppingPlacedEvent
import parfait.core.notification.port.out.NotificationOutboxAppendPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import java.time.LocalDate
import java.time.LocalDateTime

class ToppingPlacedNotifierTest {
    private val groupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val appendPort = mockk<NotificationOutboxAppendPort>(relaxed = true)
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val notifier = ToppingPlacedNotifier(groupMemberQueryPort, appendPort, eventPublisher)

    private val now = LocalDateTime.of(2026, 9, 2, 10, 0)
    private val payload =
        ToppingPlacedPayload(groupId = 1L, parfaitId = 5L, parfaitDate = LocalDate.of(2026, 9, 2), actorMemberId = 42L)

    private fun member(
        id: Long,
        memberId: Long,
        left: LocalDateTime? = null,
    ) = ParfaitGroupMember.reconstitute(
        id = id,
        parfaitGroupId = 1L,
        memberId = memberId,
        groupNickname = "닉$memberId",
        joinedAt = now,
        leftAt = left,
    )

    @Test
    fun `본인과 나간 멤버를 제외한 수신자마다 outbox 행을 만들고 이벤트를 1회 발행한다`() {
        every { groupMemberQueryPort.findAllByGroupId(1L) } returns
            listOf(
                member(10L, 42L), // 본인 (제외)
                member(11L, 43L), // 수신자
                member(12L, 44L), // 수신자
                member(13L, 45L, left = now), // 나감 (제외)
            )
        val rows = slot<List<NotificationOutbox>>()

        notifier.notify(payload, toppingId = 5L, now = now)

        verify { appendPort.saveAll(capture(rows)) }
        rows.captured shouldHaveSize 2
        rows.captured.map { it.receiverMemberId }.toSet() shouldBe setOf(43L, 44L)
        rows.captured.all { it.scheduledAt == now && it.payload == payload } shouldBe true
        verify(exactly = 1) { eventPublisher.publishEvent(ToppingPlacedEvent(5L)) }
    }

    @Test
    fun `수신자가 없으면 append 도 이벤트도 없다`() {
        every { groupMemberQueryPort.findAllByGroupId(1L) } returns listOf(member(10L, 42L))

        notifier.notify(payload, toppingId = 5L, now = now)

        verify(exactly = 0) { appendPort.saveAll(any()) }
        verify(exactly = 0) { eventPublisher.publishEvent(any()) }
    }
}
