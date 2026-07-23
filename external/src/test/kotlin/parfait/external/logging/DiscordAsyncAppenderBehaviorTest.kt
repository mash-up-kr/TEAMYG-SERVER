package parfait.external.logging

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import com.sun.net.httpserver.HttpServer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DiscordAsyncAppenderBehaviorTest {
    private lateinit var server: HttpServer

    @AfterTest
    fun tearDown() {
        if (::server.isInitialized) {
            server.stop(0)
        }
    }

    private fun buildEvent(): LoggingEvent {
        val logger = LoggerFactory.getLogger("parfait.test.SomeService") as Logger
        return LoggingEvent(Logger::class.java.name, logger, Level.ERROR, "메시지", null, null)
    }

    /**
     * webhook 서버가 응답하기 전에 일부러 [delayMillis]만큼 잠들게 만들어서,
     * "느린 네트워크 호출"을 실제 소켓 통신으로 재현한다.
     */
    private fun startDelayedServer(
        delayMillis: Long,
        receivedLatch: CountDownLatch,
        finishedLatch: CountDownLatch,
    ): HttpServer {
        val newServer = HttpServer.create(InetSocketAddress(0), 0)
        newServer.createContext("/webhook") { exchange ->
            receivedLatch.countDown()
            Thread.sleep(delayMillis)
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
            finishedLatch.countDown()
        }
        newServer.start()
        return newServer
    }

    @Test
    fun `DISCORD 어펜더를 직접 호출하면 webhook 응답이 느릴 때 호출 스레드가 그대로 멈춘다`() {
        val responseDelayMillis = 300L
        val receivedLatch = CountDownLatch(1)
        val finishedLatch = CountDownLatch(1)
        server = startDelayedServer(responseDelayMillis, receivedLatch, finishedLatch)

        val discordAppender =
            DiscordErrorAppender(webhookUrlProvider = { "http://localhost:${server.address.port}/webhook" })
        discordAppender.context = LoggerContext()
        discordAppender.name = "DISCORD"
        discordAppender.start()

        val callerElapsedMillis = measureTimeMillis { discordAppender.doAppend(buildEvent()) }
        println(
            "[동기 호출] 호출 스레드가 doAppend()에 묶여 있던 시간: ${callerElapsedMillis}ms (webhook 응답 지연: ${responseDelayMillis}ms)",
        )

        assertTrue(
            callerElapsedMillis >= responseDelayMillis,
            "AsyncAppender 없이 직접 호출했는데도 즉시 반환됨: ${callerElapsedMillis}ms " +
                "(이 테스트는 '원래는 동기라서 막힌다'는 걸 보여주는 대조군)",
        )
    }

    @Test
    fun `AsyncAppender로 감싸면 webhook 응답이 느려도 호출 스레드는 즉시 반환된다`() {
        val responseDelayMillis = 300L
        val receivedLatch = CountDownLatch(1)
        val finishedLatch = CountDownLatch(1)
        server = startDelayedServer(responseDelayMillis, receivedLatch, finishedLatch)

        val context = LoggerContext()
        val discordAppender =
            DiscordErrorAppender(webhookUrlProvider = { "http://localhost:${server.address.port}/webhook" })
        discordAppender.context = context
        discordAppender.name = "DISCORD"
        discordAppender.start()

        val asyncAppender = AsyncAppender()
        asyncAppender.context = context
        asyncAppender.name = "ASYNC_DISCORD"
        asyncAppender.isNeverBlock = true
        asyncAppender.addAppender(discordAppender)
        asyncAppender.start()

        val callerElapsedMillis = measureTimeMillis { asyncAppender.doAppend(buildEvent()) }
        println(
            "[AsyncAppender] 호출 스레드가 doAppend()에 묶여 있던 시간: ${callerElapsedMillis}ms (webhook 응답 지연: ${responseDelayMillis}ms)",
        )

        assertTrue(
            callerElapsedMillis < responseDelayMillis,
            "호출 스레드가 즉시 반환되지 않았다: ${callerElapsedMillis}ms (webhook 응답 지연 ${responseDelayMillis}ms)",
        )
        assertTrue(receivedLatch.await(1, TimeUnit.SECONDS), "워커 스레드에서 요청이 전송되지 않았다")
        assertTrue(finishedLatch.await(2, TimeUnit.SECONDS), "워커 스레드에서 실제 전송이 끝나지 않았다")
    }
}
