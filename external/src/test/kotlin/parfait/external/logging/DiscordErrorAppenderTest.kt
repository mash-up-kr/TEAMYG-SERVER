package parfait.external.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.slf4j.LoggerFactory
import kotlin.test.Test

class DiscordErrorAppenderTest {
    private fun buildEvent(level: Level): LoggingEvent {
        val logger = LoggerFactory.getLogger("parfait.test.SomeService") as Logger
        return LoggingEvent(Logger::class.java.name, logger, level, "메시지", null, null)
    }

    private fun buildAppender(
        webhookUrl: String?,
        sender: DiscordWebhookSender,
    ): DiscordErrorAppender {
        val appender = DiscordErrorAppender(webhookUrlProvider = { webhookUrl }, webhookSender = sender)
        appender.context = LoggerContext()
        appender.start()
        return appender
    }

    @Test
    fun `ERROR 레벨이고 webhookUrl이 있으면 전송을 시도한다`() {
        val sender = mockk<DiscordWebhookSender>()
        every { sender.send(any(), any()) } just Runs
        val appender = buildAppender("http://example.com/webhook", sender)

        appender.doAppend(buildEvent(Level.ERROR))

        verify(exactly = 1) { sender.send("http://example.com/webhook", any()) }
    }

    @Test
    fun `INFO 레벨이면 전송하지 않는다`() {
        val sender = mockk<DiscordWebhookSender>()
        val appender = buildAppender("http://example.com/webhook", sender)

        appender.doAppend(buildEvent(Level.INFO))

        verify(exactly = 0) { sender.send(any(), any()) }
    }

    @Test
    fun `webhookUrl이 없으면 전송하지 않는다`() {
        val sender = mockk<DiscordWebhookSender>()
        val appender = buildAppender(null, sender)

        appender.doAppend(buildEvent(Level.ERROR))

        verify(exactly = 0) { sender.send(any(), any()) }
    }

    @Test
    fun `webhookUrl이 공백뿐이면 전송하지 않는다`() {
        val sender = mockk<DiscordWebhookSender>()
        val appender = buildAppender("   ", sender)

        appender.doAppend(buildEvent(Level.ERROR))

        verify(exactly = 0) { sender.send(any(), any()) }
    }

    @Test
    fun `sender가 예외를 던져도 doAppend는 예외를 전파하지 않는다`() {
        val sender = mockk<DiscordWebhookSender>()
        every { sender.send(any(), any()) } throws RuntimeException("network boom")
        val appender = buildAppender("http://example.com/webhook", sender)

        appender.doAppend(buildEvent(Level.ERROR))
        // 여기까지 예외 없이 도달하면 성공
    }

    @Test
    fun `formatter가 예외를 던져도 doAppend는 예외를 전파하지 않고 sender는 호출되지 않는다`() {
        val sender = mockk<DiscordWebhookSender>()
        val appender = buildAppender("http://example.com/webhook", sender)
        mockkObject(DiscordMessageFormatter)
        every { DiscordMessageFormatter.format(any()) } throws RuntimeException("format boom")

        try {
            appender.doAppend(buildEvent(Level.ERROR))

            verify(exactly = 0) { sender.send(any(), any()) }
        } finally {
            unmockkObject(DiscordMessageFormatter)
        }
    }
}
