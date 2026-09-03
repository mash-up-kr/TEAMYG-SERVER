package parfait.core.notification.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.notification.domain.DevicePlatform
import parfait.core.notification.domain.DeviceToken
import parfait.core.notification.domain.NotificationMessageFactory
import parfait.core.notification.domain.NotificationOutbox
import parfait.core.notification.domain.OutboxStatus
import parfait.core.notification.domain.ToppingPlacedPayload
import parfait.core.notification.exception.NotificationSendException
import parfait.core.notification.port.out.DeviceTokenDeletePort
import parfait.core.notification.port.out.DeviceTokenQueryPort
import parfait.core.notification.port.out.NotificationOutboxPollPort
import parfait.core.notification.port.out.NotificationSenderPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroup
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import java.time.LocalDate
import java.time.LocalDateTime

class NotificationOutboxDispatcherTest {
    private val pollPort = mockk<NotificationOutboxPollPort>(relaxed = true)
    private val groupQueryPort = mockk<ParfaitGroupQueryPort>()
    private val groupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val deviceTokenQueryPort = mockk<DeviceTokenQueryPort>()
    private val deviceTokenDeletePort = mockk<DeviceTokenDeletePort>(relaxed = true)
    private val senderPort = mockk<NotificationSenderPort>(relaxed = true)
    private val dispatcher =
        NotificationOutboxDispatcher(
            pollPort,
            groupQueryPort,
            groupMemberQueryPort,
            deviceTokenQueryPort,
            deviceTokenDeletePort,
            senderPort,
            NotificationMessageFactory(),
        )

    private val now = LocalDateTime.of(2026, 9, 2, 10, 0)
    private val payload =
        ToppingPlacedPayload(groupId = 1L, parfaitId = 5L, parfaitDate = LocalDate.of(2026, 9, 2), actorMemberId = 7L)

    private fun row(
        id: Long = 1L,
        receiver: Long = 42L,
        attempts: Int = 0,
    ) = NotificationOutbox.reconstitute(
        id = id,
        aggregateType = "TOPPING",
        aggregateId = 5L,
        eventType = "TOPPING_PLACED",
        receiverMemberId = receiver,
        payload = payload,
        dedupKey = "topping-placed:5:$receiver",
        status = OutboxStatus.PENDING,
        attempts = attempts,
        scheduledAt = now,
        lastError = null,
        createdAt = now,
        sentAt = null,
    )

    private fun group() =
        ParfaitGroup.reconstitute(
            id = 1L,
            name = "우리팀",
            inviteCode = "ABCDEF",
            memberLimit = 12,
            createdAt = now,
            updatedAt = now,
        )

    private fun member(
        memberId: Long,
        left: LocalDateTime? = null,
    ) = ParfaitGroupMember.reconstitute(
        id = memberId,
        parfaitGroupId = 1L,
        memberId = memberId,
        groupNickname = "닉$memberId",
        joinedAt = now,
        leftAt = left,
    )

    private fun token(
        t: String,
        memberId: Long = 42L,
    ) = DeviceToken(token = t, memberId = memberId, platform = DevicePlatform.ANDROID)

    @Test
    fun `정상 경로 - 재검증 통과 후 발송 성공하면 markSent`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row())
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("tok-1"))

        dispatcher.processDueBatch(now)

        verify { senderPort.send("tok-1", match { it.title == "우리팀 파르페에 체리 얹을 타이밍!" && it.body == "닉7님이 새 토핑을 쌓았어요" }) }
        verify { pollPort.markSent(1L, now, null) }
    }

    @Test
    fun `E-04 그룹이 삭제됐으면 발송 없이 markSent(CANCELLED_GROUP_DELETED)`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row())
        every { groupQueryPort.findById(1L) } returns null

        dispatcher.processDueBatch(now)

        verify(exactly = 0) { senderPort.send(any(), any()) }
        verify { pollPort.markSent(1L, now, "CANCELLED_GROUP_DELETED") }
    }

    @Test
    fun `E-03 수신자가 그룹을 나갔으면 markSent(CANCELLED_RECEIVER_LEFT)`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row())
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L, left = now)

        dispatcher.processDueBatch(now)

        verify(exactly = 0) { senderPort.send(any(), any()) }
        verify { pollPort.markSent(1L, now, "CANCELLED_RECEIVER_LEFT") }
    }

    @Test
    fun `E-05 작성자가 나갔으면 본문을 익명 문구로 발송`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row())
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns null
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("tok-1"))

        dispatcher.processDueBatch(now)

        verify { senderPort.send("tok-1", match { it.body == "누군가 새 토핑을 쌓았어요" }) }
        verify { pollPort.markSent(1L, now, null) }
    }

    @Test
    fun `E-02 활성 토큰이 없으면 markSent(NO_DEVICE_TOKEN)`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row())
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns emptyList()

        dispatcher.processDueBatch(now)

        verify(exactly = 0) { senderPort.send(any(), any()) }
        verify { pollPort.markSent(1L, now, "NO_DEVICE_TOKEN") }
    }

    @Test
    fun `재시도가능 실패면 markRetry 로 다음 회차 예약`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row(attempts = 0))
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("tok-1"))
        every { senderPort.send("tok-1", any()) } throws
            NotificationSendException(retryable = true, errorCode = "UNAVAILABLE", message = "x")

        dispatcher.processDueBatch(now)

        verify { pollPort.markRetry(1L, 1, now.plusMinutes(1), any()) }
    }

    @Test
    fun `MAX_ATTEMPTS 도달 시 재시도가능 실패라도 markFailed`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row(attempts = 4))
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("tok-1"))
        every { senderPort.send("tok-1", any()) } throws
            NotificationSendException(retryable = true, errorCode = "INTERNAL", message = "x")

        dispatcher.processDueBatch(now)

        verify { pollPort.markFailed(1L, any()) }
        verify(exactly = 0) { pollPort.markRetry(any(), any(), any(), any()) }
    }

    @Test
    fun `죽은 토큰 코드면 즉시 deleteByToken 하고 markFailed`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row())
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("dead-tok"))
        every { senderPort.send("dead-tok", any()) } throws
            NotificationSendException(retryable = false, errorCode = "UNREGISTERED", message = "x")

        dispatcher.processDueBatch(now)

        verify { deviceTokenDeletePort.deleteByToken("dead-tok") }
        verify { pollPort.markFailed(1L, any()) }
    }

    @Test
    fun `기기 2대 중 1대만 성공하면 markSent`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row())
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("ok"), token("dead"))
        every { senderPort.send("ok", any()) } returns Unit
        every { senderPort.send("dead", any()) } throws
            NotificationSendException(retryable = false, errorCode = "UNREGISTERED", message = "x")

        dispatcher.processDueBatch(now)

        verify { deviceTokenDeletePort.deleteByToken("dead") }
        verify { pollPort.markSent(1L, now, null) }
    }
}
