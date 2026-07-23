package parfait.external.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.LoggingEvent
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import tools.jackson.databind.ObjectMapper
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscordMessageFormatterTest {
    private val objectMapper = ObjectMapper()

    @AfterTest
    fun tearDown() {
        MDC.clear()
    }

    private fun buildEvent(
        message: String,
        throwable: Throwable? = null,
    ): LoggingEvent {
        val logger = LoggerFactory.getLogger("parfait.test.SomeService") as Logger
        return LoggingEvent(Logger::class.java.name, logger, Level.ERROR, message, throwable, null)
    }

    @Test
    fun `traceId와 요청 URI가 MDC에서 JSON payload에 그대로 포함된다`() {
        MDC.put("traceId", "trace-123")
        MDC.put("requestUri", "GET /letters/1")
        try {
            val event = buildEvent("에러 발생", RuntimeException("boom"))

            val json = DiscordMessageFormatter.format(event)

            val description = objectMapper.readTree(json)["embeds"][0]["description"].asText()
            assertTrue(description.contains("trace-123"))
            assertTrue(description.contains("GET /letters/1"))
        } finally {
            MDC.clear()
        }
    }

    @Test
    fun `logger 이름이 embed title에 포함된다`() {
        val event = buildEvent("에러 발생", RuntimeException("boom"))

        val json = DiscordMessageFormatter.format(event)

        val title = objectMapper.readTree(json)["embeds"][0]["title"].asText()
        assertTrue(title.contains("parfait.test.SomeService"))
    }

    @Test
    fun `예외의 클래스 타입이 description에 포함된다`() {
        val event = buildEvent("에러 발생", IllegalStateException("문제 발생"))

        val json = DiscordMessageFormatter.format(event)

        val description = objectMapper.readTree(json)["embeds"][0]["description"].asText()
        assertTrue(description.contains("IllegalStateException"))
    }

    @Test
    fun `스택트레이스는 상위 5줄까지만 포함된다`() {
        val event = buildEvent("에러 발생", RuntimeException("boom"))

        val json = DiscordMessageFormatter.format(event)

        val description = objectMapper.readTree(json)["embeds"][0]["description"].asText()
        val stackLineCount =
            description
                .substringAfter("```\n")
                .substringBefore("\n```")
                .lines()
                .size
        assertEquals(5, stackLineCount)
    }

    @Test
    fun `MDC에 값이 없으면 대시로 표시된다`() {
        MDC.clear()
        val event = buildEvent("에러 발생", null)

        val json = DiscordMessageFormatter.format(event)

        val description = objectMapper.readTree(json)["embeds"][0]["description"].asText()
        assertTrue(description.contains("**traceId**: -"))
    }

    @Test
    fun `footer에 발생 시각이 라벨과 함께 표시된다`() {
        val event = buildEvent("에러 발생", RuntimeException("boom"))

        val json = DiscordMessageFormatter.format(event)

        val footerText = objectMapper.readTree(json)["embeds"][0]["footer"]["text"].asText()
        assertTrue(footerText.contains("발생 시각"))
    }
}
