package parfait.external.discord.command.help

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import parfait.external.discord.command.CommandReply
import parfait.external.discord.command.CommandRequest
import parfait.external.discord.command.DiscordCommand

class HelpCommandTest {
    private class FakeCommand(
        override val name: String,
        override val usage: String,
        override val description: String,
        override val adminOnly: Boolean,
    ) : DiscordCommand {
        override fun definition(): SlashCommandData = Commands.slash(name, description)

        override fun execute(request: CommandRequest) = CommandReply.text("", ephemeral = true)
    }

    private fun helpWith(vararg commands: DiscordCommand): HelpCommand {
        val provider = mockk<ObjectProvider<DiscordCommand>>()
        every { provider.orderedStream() } answers { commands.toList().stream() }
        return HelpCommand(provider)
    }

    private val request = CommandRequest(subcommand = null, options = emptyMap(), isAdmin = false)

    @Test
    fun `등록된 명령어를 조회와 운영진 설정으로 나눠 보여준다`() {
        val help =
            helpWith(
                FakeCommand("광고모드", "/광고모드 켜기|끄기|상태", "리포트 주기 전환", adminOnly = true),
                FakeCommand("가입자", "/가입자 [기간]", "가입·탈퇴·누적 조회", adminOnly = false),
            )

        val reply = help.execute(request)

        reply.ephemeral shouldBe true
        val embed = reply.embed!!
        embed.title shouldBe "🤖 지표봇 명령어"
        embed.fields.map { it.name } shouldBe listOf("📊 조회", "⚙️ 설정 (운영진만)")
        embed.fields[0].value shouldBe "`/가입자 [기간]` — 가입·탈퇴·누적 조회"
        embed.fields[1].value shouldBe "`/광고모드 켜기|끄기|상태` — 리포트 주기 전환"
    }

    @Test
    fun `운영진 전용 명령어가 없으면 설정 필드를 만들지 않는다`() {
        val help = helpWith(FakeCommand("가입자", "/가입자 [기간]", "가입·탈퇴·누적 조회", adminOnly = false))

        help
            .execute(request)
            .embed!!
            .fields
            .map { it.name } shouldBe listOf("📊 조회")
    }
}
