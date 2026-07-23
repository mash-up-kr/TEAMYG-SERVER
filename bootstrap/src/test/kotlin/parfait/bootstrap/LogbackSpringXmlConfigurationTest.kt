package parfait.bootstrap

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import org.slf4j.Logger.ROOT_LOGGER_NAME
import parfait.external.logging.DiscordErrorAppender
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LogbackSpringXmlConfigurationTest {
    @Test
    fun `DISCORD 어펜더는 AsyncAppender로 감싸인 채로 root에 연결된다`() {
        val context = LoggerContext()
        val configurator = JoranConfigurator()
        configurator.context = context

        javaClass.getResourceAsStream("/logback-spring.xml")!!.use { input ->
            configurator.doConfigure(input)
        }

        val root = context.getLogger(ROOT_LOGGER_NAME)
        val rootAppenderNames =
            root
                .iteratorForAppenders()
                .asSequence()
                .map { it.name }
                .toList()

        assertFalse(
            rootAppenderNames.contains("DISCORD"),
            "DISCORD가 root에 직접 연결되면 안 된다 — 동기 경로로 되돌아가는 회귀",
        )
        assertTrue(rootAppenderNames.contains("ASYNC_DISCORD"))

        val asyncDiscord = root.getAppender("ASYNC_DISCORD") as AsyncAppender
        assertTrue(asyncDiscord.isNeverBlock, "neverBlock이 꺼지면 큐가 찼을 때 다시 호출 스레드가 막힌다")

        val wrapped = asyncDiscord.iteratorForAppenders().asSequence().single()
        assertIs<DiscordErrorAppender>(wrapped)
    }
}
