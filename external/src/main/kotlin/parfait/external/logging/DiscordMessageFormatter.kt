package parfait.external.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DiscordMessageFormatter {
    private const val MAX_STACK_TRACE_LINES = 5
    private const val EMBED_COLOR_RED = 15158332
    private val objectMapper = ObjectMapper()
    private val timestampFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"))

    fun format(event: ILoggingEvent): String {
        val timestamp = timestampFormatter.format(Instant.ofEpochMilli(event.timeStamp))
        val traceId = event.mdcPropertyMap["traceId"] ?: "-"
        val requestUri = event.mdcPropertyMap["requestUri"] ?: "-"
        val exceptionType = event.throwableProxy?.className ?: "-"
        val profile = System.getenv("SPRING_PROFILES_ACTIVE") ?: "-"
        val stackTrace =
            event.throwableProxy
                ?.stackTraceElementProxyArray
                ?.take(MAX_STACK_TRACE_LINES)
                ?.joinToString("\n") { it.stackTraceElement.toString() }
                ?.ifEmpty { null }
                ?: "-"

        val header =
            """
            **traceId**: $traceId
            **요청**: $requestUri
            **예외 타입**: $exceptionType
            **프로파일**: $profile
            **메시지**: ${event.formattedMessage}
            """.trimIndent()
        val description = "$header\n```\n$stackTrace\n```"

        val payload =
            mapOf(
                "embeds" to
                    listOf(
                        mapOf(
                            "title" to "🚨 에러 발생: ${event.loggerName}",
                            "description" to description,
                            "color" to EMBED_COLOR_RED,
                            "footer" to mapOf("text" to "발생 시각: $timestamp"),
                        ),
                    ),
            )

        return objectMapper.writeValueAsString(payload)
    }
}
