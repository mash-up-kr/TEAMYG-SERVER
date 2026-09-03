package parfait.external.notification

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.SendResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.notification.domain.PushMessage
import parfait.core.notification.exception.NotificationSendException
import java.time.Duration

class FcmNotificationSenderTest {
    private val firebaseMessaging = mockk<FirebaseMessaging>()
    private val sender = FcmNotificationSender(firebaseMessaging)

    private val message =
        PushMessage(
            title = "제목",
            body = "본문",
            data = mapOf("type" to "TOPPING", "route" to "canvas", "groupId" to "50", "date" to "2026-09-02"),
            ttl = Duration.ofHours(6),
        )

    @Test
    fun `send 가 FirebaseMessaging_send 를 예외 없이 1회 호출한다`() {
        val sent = slot<Message>()
        every { firebaseMessaging.send(capture(sent)) } returns "projects/x/messages/1"

        sender.send("tok-1", message)

        verify(exactly = 1) { firebaseMessaging.send(any()) }
        sent.isCaptured shouldBe true
    }

    @Test
    fun `UNREGISTERED 는 retryable=false, errorCode 를 담은 NotificationSendException`() {
        val fcmEx = mockk<FirebaseMessagingException>()
        every { fcmEx.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
        every { fcmEx.message } returns "requested entity was not found"
        every { firebaseMessaging.send(any()) } throws fcmEx

        val ex = shouldThrow<NotificationSendException> { sender.send("dead", message) }
        ex.retryable shouldBe false
        ex.errorCode shouldBe "UNREGISTERED"
    }

    @Test
    fun `messagingErrorCode 가 null(타임아웃·네트워크)이면 retryable=true, errorCode=null`() {
        val fcmEx = mockk<FirebaseMessagingException>()
        every { fcmEx.messagingErrorCode } returns null
        every { fcmEx.message } returns "Timed out waiting for FCM response"
        every { firebaseMessaging.send(any()) } throws fcmEx

        val ex = shouldThrow<NotificationSendException> { sender.send("tok", message) }
        ex.retryable shouldBe true
        ex.errorCode shouldBe null
    }

    @Test
    fun `UNAVAILABLE 은 retryable=true`() {
        val fcmEx = mockk<FirebaseMessagingException>()
        every { fcmEx.messagingErrorCode } returns MessagingErrorCode.UNAVAILABLE
        every { fcmEx.message } returns "backend unavailable"
        every { firebaseMessaging.send(any()) } throws fcmEx

        val ex = shouldThrow<NotificationSendException> { sender.send("tok", message) }
        ex.retryable shouldBe true
        ex.errorCode shouldBe "UNAVAILABLE"
    }

    @Test
    fun `sendMulticast - 부분 실패를 인덱스로 토큰에 역매핑한다`() {
        val ok =
            mockk<SendResponse> {
                every { isSuccessful } returns true
                every { exception } returns null
            }
        val deadEx =
            mockk<FirebaseMessagingException> {
                every { messagingErrorCode } returns
                    MessagingErrorCode.UNREGISTERED
            }
        val dead =
            mockk<SendResponse> {
                every { isSuccessful } returns false
                every { exception } returns deadEx
            }
        val batch = mockk<BatchResponse> { every { responses } returns listOf(ok, dead) }
        every { firebaseMessaging.sendEachForMulticast(any()) } returns batch

        val result = sender.sendMulticast(listOf("ok-tok", "dead-tok"), message)

        result.successCount shouldBe 1
        result.deadTokens() shouldContainExactly listOf("dead-tok")
    }

    @Test
    fun `sendMulticast - 전체 실패면 NotificationSendException`() {
        val fcmEx = mockk<FirebaseMessagingException>()
        every { fcmEx.messagingErrorCode } returns MessagingErrorCode.UNAVAILABLE
        every { fcmEx.message } returns "backend unavailable"
        every { firebaseMessaging.sendEachForMulticast(any()) } throws fcmEx

        val ex = shouldThrow<NotificationSendException> { sender.sendMulticast(listOf("t1"), message) }
        ex.retryable shouldBe true
        ex.errorCode shouldBe "UNAVAILABLE"
    }

    @Test
    fun `sendMulticast - 500개 초과면 IllegalArgumentException`() {
        shouldThrow<IllegalArgumentException> { sender.sendMulticast((1..501).map { "t$it" }, message) }
    }
}
