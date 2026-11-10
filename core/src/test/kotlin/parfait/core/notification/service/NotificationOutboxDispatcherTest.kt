package parfait.core.notification.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import parfait.core.notification.domain.ActivityDigestPayload
import parfait.core.notification.domain.DevicePlatform
import parfait.core.notification.domain.DeviceToken
import parfait.core.notification.domain.NotificationMessageFactory
import parfait.core.notification.domain.NotificationOutbox
import parfait.core.notification.domain.NotificationSettings
import parfait.core.notification.domain.OutboxStatus
import parfait.core.notification.domain.TextPlacedPayload
import parfait.core.notification.domain.ToppingPlacedPayload
import parfait.core.notification.exception.NotificationSendException
import parfait.core.notification.port.out.DeviceTokenDeletePort
import parfait.core.notification.port.out.DeviceTokenQueryPort
import parfait.core.notification.port.out.NotificationOutboxDigestPort
import parfait.core.notification.port.out.NotificationOutboxPollPort
import parfait.core.notification.port.out.NotificationSenderPort
import parfait.core.notification.port.out.NotificationSettingsQueryPort
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
    private val settingsQueryPort = mockk<NotificationSettingsQueryPort>()
    private val digestPort = mockk<NotificationOutboxDigestPort>(relaxed = true)
    private val coordinator = mockk<ActivityDigestCoordinator>()
    private val dispatcher =
        NotificationOutboxDispatcher(
            pollPort,
            groupQueryPort,
            groupMemberQueryPort,
            deviceTokenQueryPort,
            deviceTokenDeletePort,
            senderPort,
            NotificationMessageFactory(),
            coordinator,
            settingsQueryPort,
            digestPort,
        )

    @BeforeEach
    fun defaultDecision() {
        every { coordinator.decide(any(), any()) } returns DigestDecision.SEND_NOW
    }

    private val now = LocalDateTime.of(2026, 9, 2, 10, 0)
    private val payload =
        ToppingPlacedPayload(groupId = 1L, parfaitId = 5L, parfaitDate = LocalDate.of(2026, 9, 2), actorMemberId = 7L)

    private fun row(
        id: Long = 1L,
        receiver: Long = 42L,
        attempts: Int = 0,
        eventType: String = "TOPPING_PLACED",
    ) = NotificationOutbox.reconstitute(
        id = id,
        aggregateType = "TOPPING",
        aggregateId = 5L,
        eventType = eventType,
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

    private val textPayload =
        TextPlacedPayload(
            groupId = 1L,
            parfaitId = 5L,
            parfaitDate = LocalDate.of(2026, 9, 2),
            actorMemberId = 7L,
            text = "오늘 다들 뭐 먹었어",
        )

    private fun textRow(
        id: Long = 2L,
        receiver: Long = 42L,
    ) = NotificationOutbox.reconstitute(
        id = id,
        aggregateType = "TEXT",
        aggregateId = 5L,
        eventType = "TEXT_PLACED",
        receiverMemberId = receiver,
        payload = textPayload,
        dedupKey = "text-placed:5:$receiver",
        status = OutboxStatus.PENDING,
        attempts = 0,
        scheduledAt = now,
        lastError = null,
        createdAt = now,
        sentAt = null,
    )

    @Test
    fun `정상 경로 - 재검증 통과 후 발송 성공하면 markSent`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row())
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("tok-1"))

        dispatcher.processDueBatch(now)

        verify { senderPort.send("tok-1", match { it.title == "우리팀 파르페에 체리 하나 톡!" && it.body == "닉7님이 새 사진을 쌓았어요" }) }
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

        verify { senderPort.send("tok-1", match { it.body == "누군가 새 사진을 쌓았어요" }) }
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

    @Test
    fun `취소 사유가 cancelledByReason 에 사유별로 집계된다`() {
        every { pollPort.claimBatch(any(), now) } returns
            listOf(row(id = 1L, receiver = 11L), row(id = 2L, receiver = 22L))
        every { groupQueryPort.findById(1L) } returnsMany listOf(null, group())
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 22L) } returns member(22L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(22L) } returns emptyList()

        val outcome = dispatcher.processDueBatch(now)

        outcome.cancelled shouldBe 2
        outcome.cancelledByReason shouldBe mapOf("CANCELLED_GROUP_DELETED" to 1, "NO_DEVICE_TOKEN" to 1)
    }

    @Test
    fun `BACKGROUND_CHANGED 행은 배경 변경 문구로 발송한다`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row(eventType = "BACKGROUND_CHANGED"))
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("tok-1"))

        dispatcher.processDueBatch(now)

        verify { senderPort.send("tok-1", match { it.title == "우리팀 파르페에 체리 하나 톡!" && it.body == "닉7님이 배경을 바꿨어요" }) }
        verify { pollPort.markSent(1L, now, null) }
    }

    @Test
    fun `알 수 없는 eventType 은 markFailed 하고 다음 행을 계속 처리한다`() {
        every { pollPort.claimBatch(any(), now) } returns
            listOf(row(id = 1L, eventType = "UNKNOWN"), row(id = 2L))
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("tok-1"))

        dispatcher.processDueBatch(now)

        verify { pollPort.markFailed(1L, match { it.startsWith("UNKNOWN_EVENT_TYPE") }) }
        verify { pollPort.markSent(2L, now, null) }
        verify(exactly = 1) { senderPort.send("tok-1", any()) }
    }

    @Test
    fun `TEXT_PLACED - 정상 발송 성공 후 markSent`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(textRow())
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("tok-1"))

        dispatcher.processDueBatch(now)

        verify {
            senderPort.send(
                "tok-1",
                match {
                    it.title == "우리팀 파르페에 체리 하나 톡!" &&
                        it.body == "닉7: 오늘 다들 뭐 먹었어"
                },
            )
        }
        verify { pollPort.markSent(2L, now, null) }
    }

    @Test
    fun `TEXT_PLACED - E-22 정규화 후 빈 문자열이면 발송하지 않는다`() {
        val emptyPayload =
            TextPlacedPayload(
                groupId = 1L,
                parfaitId = 5L,
                parfaitDate = LocalDate.of(2026, 9, 2),
                actorMemberId = 7L,
                text = "  \n  ",
            )
        val emptyRow =
            NotificationOutbox.reconstitute(
                id = 2L,
                aggregateType = "TEXT",
                aggregateId = 5L,
                eventType = "TEXT_PLACED",
                receiverMemberId = 42L,
                payload = emptyPayload,
                dedupKey = "text-placed:5:42",
                status = OutboxStatus.PENDING,
                attempts = 0,
                scheduledAt = now,
                lastError = null,
                createdAt = now,
                sentAt = null,
            )
        every { pollPort.claimBatch(any(), now) } returns listOf(emptyRow)
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("tok-1"))

        dispatcher.processDueBatch(now)

        verify(exactly = 0) { senderPort.send(any(), any()) }
        verify { pollPort.markSent(2L, now, "CANCELLED_EMPTY_PREVIEW") }
    }

    @Test
    fun `빈 텍스트 행은 coordinator decide 를 호출하지 않고 CANCELLED_EMPTY_PREVIEW 로 취소된다`() {
        val emptyRow =
            NotificationOutbox.reconstitute(
                id = 2L,
                aggregateType = "TEXT",
                aggregateId = 5L,
                eventType = "TEXT_PLACED",
                receiverMemberId = 42L,
                payload = textPayload.copy(text = "  \n  "),
                dedupKey = "text-placed:5:42",
                status = OutboxStatus.PENDING,
                attempts = 0,
                scheduledAt = now,
                lastError = null,
                createdAt = now,
                sentAt = null,
            )
        every { pollPort.claimBatch(any(), now) } returns listOf(emptyRow)
        arrangeValidReceiver()

        dispatcher.processDueBatch(now)

        verify(exactly = 0) { coordinator.decide(any(), any()) }
        verify(exactly = 0) { senderPort.send(any(), any()) }
        verify { pollPort.markSent(2L, now, "CANCELLED_EMPTY_PREVIEW") }
    }

    private fun digestRow(id: Long = 9L) =
        NotificationOutbox.reconstitute(
            id = id,
            aggregateType = "GROUP",
            aggregateId = 1L,
            eventType = "ACTIVITY_DIGEST",
            receiverMemberId = 42L,
            payload = ActivityDigestPayload(1L, 5L, LocalDate.of(2026, 9, 2), 7L),
            dedupKey = "activity-digest:42:1:2026-09-02:20260902100000",
            status = OutboxStatus.PENDING,
            attempts = 0,
            scheduledAt = now,
            lastError = null,
            createdAt = now,
            sentAt = null,
        )

    private fun arrangeValidReceiver() {
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns listOf(token("tok-1"))
    }

    @Test
    fun `묶음 모드에서 윈도우가 열려 있으면 발송하지 않고 미룬다`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(row())
        arrangeValidReceiver()
        every { coordinator.decide(any(), now) } returns DigestDecision.DEFER

        dispatcher.processDueBatch(now)

        verify(exactly = 0) { senderPort.send(any(), any()) }
        verify(exactly = 0) { pollPort.markSent(any(), any(), any()) }
    }

    @Test
    fun `P-06 예약 행은 재집계한 개수로 발송하고 대기 행을 정리한다`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(digestRow())
        arrangeValidReceiver()
        every { settingsQueryPort.findByMemberId(42L) } returns NotificationSettings(true, true)
        every { coordinator.countAlive("activity-digest:42:1:2026-09-02:20260902100000") } returns 3

        dispatcher.processDueBatch(now)

        verify { senderPort.send("tok-1", match { it.body == "그사이 새 소식이 3개 쌓였어요" }) }
        verify { pollPort.markSent(9L, now, null) }
        verify { digestPort.resolveDeferred("activity-digest:42:1:2026-09-02:20260902100000", now, "DIGESTED") }
    }

    @Test
    fun `P-06 예약 행은 개수가 100 이상이면 99+로 발송한다`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(digestRow())
        arrangeValidReceiver()
        every { settingsQueryPort.findByMemberId(42L) } returns NotificationSettings(true, true)
        every { coordinator.countAlive(any()) } returns 150

        dispatcher.processDueBatch(now)

        verify { senderPort.send("tok-1", match { it.body == "그사이 새 소식이 99+개 쌓였어요" }) }
    }

    @Test
    fun `E-26 재집계 결과가 0이면 발송 없이 취소하고 대기 행을 정리한다`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(digestRow())
        arrangeValidReceiver()
        every { settingsQueryPort.findByMemberId(42L) } returns NotificationSettings(true, true)
        every { coordinator.countAlive(any()) } returns 0

        dispatcher.processDueBatch(now)

        verify(exactly = 0) { senderPort.send(any(), any()) }
        verify { pollPort.markSent(9L, now, "CANCELLED_EMPTY_DIGEST") }
        verify {
            digestPort.resolveDeferred(
                "activity-digest:42:1:2026-09-02:20260902100000",
                now,
                "CANCELLED_EMPTY_DIGEST",
            )
        }
    }

    @Test
    fun `E-27 모아 받기를 껐으면 발송 없이 취소하고 대기 행을 정리한다`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(digestRow())
        arrangeValidReceiver()
        every { settingsQueryPort.findByMemberId(42L) } returns NotificationSettings(true, false)

        dispatcher.processDueBatch(now)

        verify(exactly = 0) { senderPort.send(any(), any()) }
        verify { pollPort.markSent(9L, now, "CANCELLED_DIGEST_DISABLED") }
        verify {
            digestPort.resolveDeferred(
                "activity-digest:42:1:2026-09-02:20260902100000",
                now,
                "CANCELLED_DIGEST_DISABLED",
            )
        }
    }

    @Test
    fun `E-25 수신자가 그룹을 나갔으면 예약 행을 취소하고 대기 행도 정리한다`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(digestRow())
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L, left = now)

        dispatcher.processDueBatch(now)

        verify { pollPort.markSent(9L, now, "CANCELLED_RECEIVER_LEFT") }
        verify {
            digestPort.resolveDeferred(
                "activity-digest:42:1:2026-09-02:20260902100000",
                now,
                "CANCELLED_RECEIVER_LEFT",
            )
        }
    }

    private val digestKey = "activity-digest:42:1:2026-09-02:20260902100000"

    @Test
    fun `P-06 예약 행이 비재시도 실패로 끝나면 markFailed 하고 대기 행을 DIGEST_FAILED 로 정리한다`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(digestRow())
        arrangeValidReceiver()
        every { settingsQueryPort.findByMemberId(42L) } returns NotificationSettings(true, true)
        every { coordinator.countAlive(any()) } returns 3
        every { senderPort.send("tok-1", any()) } throws
            NotificationSendException(retryable = false, errorCode = "INVALID_ARGUMENT", message = "x")

        dispatcher.processDueBatch(now)

        verify { pollPort.markFailed(9L, any()) }
        verify { digestPort.resolveDeferred(digestKey, now, "DIGEST_FAILED") }
    }

    @Test
    fun `P-06 예약 행의 수신자에게 토큰이 없으면 대기 행을 NO_DEVICE_TOKEN 으로 정리한다`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(digestRow())
        every { groupQueryPort.findById(1L) } returns group()
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns member(42L)
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 7L) } returns member(7L)
        every { deviceTokenQueryPort.findByMemberId(42L) } returns emptyList()

        dispatcher.processDueBatch(now)

        verify { pollPort.markSent(9L, now, "NO_DEVICE_TOKEN") }
        verify { digestPort.resolveDeferred(digestKey, now, "NO_DEVICE_TOKEN") }
    }

    @Test
    fun `P-06 예약 행의 그룹이 삭제됐으면 대기 행을 CANCELLED_GROUP_DELETED 로 정리한다`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(digestRow())
        every { groupQueryPort.findById(1L) } returns null

        dispatcher.processDueBatch(now)

        verify { pollPort.markSent(9L, now, "CANCELLED_GROUP_DELETED") }
        verify { digestPort.resolveDeferred(digestKey, now, "CANCELLED_GROUP_DELETED") }
    }

    @Test
    fun `P-06 예약 행이 재시도 가능한 실패이면 markRetry 만 하고 대기 행은 정리하지 않는다`() {
        every { pollPort.claimBatch(any(), now) } returns listOf(digestRow())
        arrangeValidReceiver()
        every { settingsQueryPort.findByMemberId(42L) } returns NotificationSettings(true, true)
        every { coordinator.countAlive(any()) } returns 3
        every { senderPort.send("tok-1", any()) } throws
            NotificationSendException(retryable = true, errorCode = "UNAVAILABLE", message = "x")

        dispatcher.processDueBatch(now)

        verify { pollPort.markRetry(9L, 1, now.plusMinutes(1), any()) }
        verify(exactly = 0) { digestPort.resolveDeferred(any(), any(), any()) }
    }
}
