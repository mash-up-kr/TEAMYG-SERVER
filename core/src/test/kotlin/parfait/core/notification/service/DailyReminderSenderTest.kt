package parfait.core.notification.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.notification.domain.MulticastResult
import parfait.core.notification.domain.NotificationMessageFactory
import parfait.core.notification.domain.ReminderType
import parfait.core.notification.domain.TokenSendResult
import parfait.core.notification.port.`in`.ReminderSendOutcome
import parfait.core.notification.port.out.DeviceTokenDeletePort
import parfait.core.notification.port.out.NotificationSenderPort
import parfait.core.notification.port.out.ReminderTargetQueryPort
import java.time.LocalDateTime

class DailyReminderSenderTest {
    private val targetQueryPort = mockk<ReminderTargetQueryPort>()
    private val senderPort = mockk<NotificationSenderPort>()
    private val deviceTokenDeletePort = mockk<DeviceTokenDeletePort>(relaxed = true)
    private val sender =
        DailyReminderSender(
            targetQueryPort,
            senderPort,
            deviceTokenDeletePort,
            NotificationMessageFactory(),
            chunkDelayMillis = 0L,
        )

    private fun allOk(tokens: List<String>) =
        MulticastResult(tokens.map { TokenSendResult(it, success = true, errorCode = null) })

    @Test
    fun `대상이 없으면 아무것도 하지 않는다`() {
        every { targetQueryPort.findActiveGroupMemberDeviceTokens() } returns emptyList()

        val outcome = sender.send(ReminderType.MORNING, LocalDateTime.of(2026, 9, 3, 10, 0))

        outcome shouldBe ReminderSendOutcome(skipped = false, targeted = 0, sent = 0, failed = 0, deadTokensDeleted = 0)
        verify(exactly = 0) { senderPort.sendMulticast(any(), any()) }
    }

    @Test
    fun `정상 발송 - 성공 개수를 집계한다`() {
        val tokens = listOf("t1", "t2", "t3")
        every { targetQueryPort.findActiveGroupMemberDeviceTokens() } returns tokens
        every { senderPort.sendMulticast(tokens, any()) } returns allOk(tokens)

        val outcome = sender.send(ReminderType.MORNING, LocalDateTime.of(2026, 9, 3, 10, 0))

        outcome.targeted shouldBe 3
        outcome.sent shouldBe 3
        outcome.skipped shouldBe false
    }

    @Test
    fun `500개 초과면 여러 청크로 나눠 호출한다`() {
        val tokens = (1..501).map { "t$it" }
        every { targetQueryPort.findActiveGroupMemberDeviceTokens() } returns tokens
        every { senderPort.sendMulticast(match { it.size == 500 }, any()) } returns allOk(tokens.take(500))
        every { senderPort.sendMulticast(match { it.size == 1 }, any()) } returns allOk(tokens.drop(500))

        sender.send(ReminderType.MORNING, LocalDateTime.of(2026, 9, 3, 10, 0))

        verify(exactly = 2) { senderPort.sendMulticast(any(), any()) }
    }

    @Test
    fun `죽은 토큰은 버퍼 후 일괄 삭제한다`() {
        val tokens = listOf("ok", "dead-1", "dead-2")
        every { targetQueryPort.findActiveGroupMemberDeviceTokens() } returns tokens
        every { senderPort.sendMulticast(tokens, any()) } returns
            MulticastResult(
                listOf(
                    TokenSendResult("ok", success = true, errorCode = null),
                    TokenSendResult("dead-1", success = false, errorCode = "UNREGISTERED"),
                    TokenSendResult("dead-2", success = false, errorCode = "INVALID_ARGUMENT"),
                ),
            )
        every { deviceTokenDeletePort.deleteByTokenIn(match { it.toSet() == setOf("dead-1", "dead-2") }) } returns 2

        val outcome = sender.send(ReminderType.MORNING, LocalDateTime.of(2026, 9, 3, 10, 0))

        outcome.deadTokensDeleted shouldBe 2
        outcome.failed shouldBe 2
    }

    @Test
    fun `P-03 은 시작 시각이 21시 이후면 발송을 건너뛴다`() {
        val outcome = sender.send(ReminderType.EVENING, LocalDateTime.of(2026, 9, 3, 21, 0))

        outcome.skipped shouldBe true
        outcome.targeted shouldBe 0
        verify(exactly = 0) { targetQueryPort.findActiveGroupMemberDeviceTokens() }
        verify(exactly = 0) { senderPort.sendMulticast(any(), any()) }
    }

    @Test
    fun `P-03 은 시작 시각이 21시 이전이면 정상 발송한다`() {
        val tokens = listOf("t1")
        every { targetQueryPort.findActiveGroupMemberDeviceTokens() } returns tokens
        every { senderPort.sendMulticast(tokens, any()) } returns allOk(tokens)

        val outcome = sender.send(ReminderType.EVENING, LocalDateTime.of(2026, 9, 3, 20, 59, 59))

        outcome.skipped shouldBe false
        outcome.sent shouldBe 1
    }

    @Test
    fun `P-02 는 시각과 무관하게 하드컷이 없다`() {
        val tokens = listOf("t1")
        every { targetQueryPort.findActiveGroupMemberDeviceTokens() } returns tokens
        every { senderPort.sendMulticast(tokens, any()) } returns allOk(tokens)

        val outcome = sender.send(ReminderType.MORNING, LocalDateTime.of(2026, 9, 3, 23, 0))

        outcome.skipped shouldBe false
        outcome.sent shouldBe 1
    }
}
