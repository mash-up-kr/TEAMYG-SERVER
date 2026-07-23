package parfait.external.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.slf4j.LoggerFactory

class DiscordErrorAppender
    @JvmOverloads
    constructor(
        private val webhookUrlProvider: () -> String? = { System.getenv("DISCORD_ERROR_WEBHOOK_URL") },
        private val webhookSender: DiscordWebhookSender = DiscordWebhookSender(),
    ) : AppenderBase<ILoggingEvent>() {
        private val log = LoggerFactory.getLogger(DiscordErrorAppender::class.java)

        override fun append(event: ILoggingEvent) {
            try {
                if (event.level != Level.ERROR) return

                val webhookUrl = webhookUrlProvider()
                if (webhookUrl.isNullOrBlank()) return

                webhookSender.send(webhookUrl, DiscordMessageFormatter.format(event))
            } catch (e: Exception) {
                log.warn("Discord 에러 알림 처리 중 예외 발생", e)
            }
        }
    }
