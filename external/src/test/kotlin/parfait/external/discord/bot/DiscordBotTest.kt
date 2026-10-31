package parfait.external.discord.bot

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import parfait.core.report.domain.ReportContent
import parfait.core.report.domain.ReportPeriod
import parfait.core.stats.domain.SignupStats
import parfait.external.discord.report.ReportEmbedRenderer
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DiscordBotTest {
    private val now = LocalDateTime.of(2026, 10, 30, 21, 0)
    private val content =
        ReportContent(
            ReportPeriod(now.minusDays(3), now),
            SignupStats(newSignups = 1, withdrawals = 0, newSignupWithdrawals = 0, totalUsers = 10),
        )
    private val router = mockk<CommandRouter>()
    private val registrar = mockk<CommandRegistrar>()
    private val calls = AtomicInteger()
    private val jda = mockk<JDA>(relaxed = true)

    private fun <T : Any> provider(value: T): ObjectProvider<T> =
        mockk {
            every { getObject() } returns value
            every { getIfAvailable(any()) } answers { value }
        }

    private fun bot(
        channelId: String = "1",
        factory: JdaFactory = JdaFactory { _, _ -> jda.also { calls.incrementAndGet() } },
    ) = DiscordBot("token", channelId, ReportEmbedRenderer(), provider(factory), provider(router), provider(registrar))

    @Test
    fun `채널 ID가 공백이면 전송이 IllegalStateException 으로 실패한다`() {
        val bot = bot(channelId = " ").also { it.start() }

        shouldThrow<IllegalStateException> { bot.send(content) }
    }

    @Test
    fun `start 전에는 전송이 실패한다`() {
        shouldThrow<IllegalStateException> { bot().send(content) }
    }

    @Test
    fun `연결 실패로 jda 가 없으면 전송이 실패하고 start 는 예외를 전파하지 않는다`() {
        val bot = bot(factory = { _, _ -> error("연결 실패") })

        bot.start()

        shouldThrow<IllegalStateException> { bot.send(content) }
    }

    @Test
    fun `연결 상태가 CONNECTED 가 아니면 즉시 실패한다`() {
        every { jda.status } returns JDA.Status.CONNECTING_TO_WEBSOCKET
        val bot = bot().also { it.start() }

        shouldThrow<IllegalStateException> { bot.send(content) }
        verify(exactly = 0) { jda.getTextChannelById(any<String>()) }
    }

    @Test
    fun `start 를 두 번 불러도 JDA 는 한 번만 만든다`() {
        val bot = bot()

        bot.start()
        bot.start()

        calls.get() shouldBe 1
    }

    @Test
    fun `리스너로 라우터와 등록기를 넘긴다`() {
        var received: List<ListenerAdapter> = emptyList()
        bot(factory = { _, listeners -> jda.also { received = listeners } }).start()

        received shouldBe listOf(router, registrar)
    }

    @Test
    fun `채널을 찾지 못하면 실패한다`() {
        every { jda.status } returns JDA.Status.CONNECTED
        every { jda.getTextChannelById("1") } returns null
        val bot = bot().also { it.start() }

        shouldThrow<IllegalStateException> { bot.send(content) }
    }

    @Test
    fun `연결된 상태에서는 채널에 임베드를 타임아웃과 함께 전송한다`() {
        val action = mockk<MessageCreateAction>()
        val channel = mockk<TextChannel>()
        every { jda.status } returns JDA.Status.CONNECTED
        every { jda.getTextChannelById("1") } returns channel
        every { channel.sendMessageEmbeds(any<MessageEmbed>()) } returns action
        every { action.timeout(10, TimeUnit.SECONDS) } returns action
        every { action.complete() } returns mockk(relaxed = true)
        val bot = bot().also { it.start() }

        bot.send(content)

        verify(exactly = 1) { action.complete() }
    }
}
