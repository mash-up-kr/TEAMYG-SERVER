package parfait.external.logging

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscordWebhookSenderTest {
    private lateinit var server: HttpServer
    private val sender = DiscordWebhookSender()

    @AfterTest
    fun tearDown() {
        if (::server.isInitialized) {
            server.stop(0)
        }
    }

    @Test
    fun `Webhook 서버가 204를 응답하면 예외 없이 정상 종료되고 요청 본문이 그대로 전달된다`() {
        val receivedBody = StringBuilder()
        val latch = CountDownLatch(1)
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/webhook") { exchange ->
            receivedBody.append(exchange.requestBody.bufferedReader().readText())
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
            latch.countDown()
        }
        server.start()

        sender.send("http://localhost:${server.address.port}/webhook", """{"content":"hello"}""")

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertEquals("""{"content":"hello"}""", receivedBody.toString())
    }

    @Test
    fun `Webhook 서버가 500을 응답해도 예외를 던지지 않는다`() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/webhook") { exchange ->
            exchange.sendResponseHeaders(500, -1)
            exchange.close()
        }
        server.start()

        sender.send("http://localhost:${server.address.port}/webhook", """{"content":"hello"}""")
        // 여기까지 예외 없이 도달하면 성공
    }

    @Test
    fun `연결할 수 없는 주소로 전송해도 예외를 던지지 않는다`() {
        sender.send("http://localhost:1/webhook", """{"content":"hello"}""")
        // 여기까지 예외 없이 도달하면 성공
    }
}
