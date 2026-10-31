package parfait.external.discord.command

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.junit.jupiter.api.Test

class CommandDispatcherTest {
    private class FakeCommand(
        override val name: String,
        override val adminOnly: Boolean = false,
    ) : DiscordCommand {
        override val usage = "/$name"
        override val description = "$name 설명"

        override fun definition(): SlashCommandData = Commands.slash(name, description)

        override fun execute(request: CommandRequest) = CommandReply.text("$name 실행", ephemeral = true)
    }

    private val dispatcher = CommandDispatcher(listOf(FakeCommand("가입자"), FakeCommand("광고모드", adminOnly = true)))

    private fun request(isAdmin: Boolean) = CommandRequest(subcommand = null, options = emptyMap(), isAdmin = isAdmin)

    @Test
    fun `이름이 맞는 명령어를 실행한다`() {
        dispatcher.dispatch("가입자", request(isAdmin = false)).text shouldBe "가입자 실행"
    }

    @Test
    fun `운영진 전용 명령어는 운영진이 아니면 거절한다`() {
        val reply = dispatcher.dispatch("광고모드", request(isAdmin = false))

        reply.text shouldBe "운영진만 사용할 수 있는 명령어입니다"
        reply.ephemeral shouldBe true
    }

    @Test
    fun `운영진이면 운영진 전용 명령어를 실행한다`() {
        dispatcher.dispatch("광고모드", request(isAdmin = true)).text shouldBe "광고모드 실행"
    }

    @Test
    fun `모르는 명령어면 안내 메시지를 돌려준다`() {
        val reply = dispatcher.dispatch("없는명령", request(isAdmin = true))

        reply.text shouldBe "알 수 없는 명령어입니다"
        reply.ephemeral shouldBe true
    }

    @Test
    fun `같은 이름의 명령어가 둘 이상이면 생성 시점에 실패한다`() {
        val e =
            shouldThrow<IllegalArgumentException> {
                CommandDispatcher(listOf(FakeCommand("광고모드", adminOnly = true), FakeCommand("광고모드")))
            }

        e.message shouldContain "광고모드"
    }

    @Test
    fun `명령어가 없으면 어떤 이름이든 알 수 없는 명령어다`() {
        CommandDispatcher(emptyList()).dispatch("가입자", request(isAdmin = true)).text shouldBe "알 수 없는 명령어입니다"
    }
}
