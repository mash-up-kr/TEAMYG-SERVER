package parfait.external.discord.command.help

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import parfait.external.discord.command.CommandReply
import parfait.external.discord.command.CommandRequest
import parfait.external.discord.command.DiscordCommand

/**
 * 등록된 [DiscordCommand] 목록에서 도움말을 자동 생성한다 — 명령어를 추가하면 도움말에도 자동 반영된다.
 * 자기 자신을 포함한 전체 목록이 필요하므로 생성자 순환을 피하려고 [ObjectProvider]로 실행 시점에 조회한다.
 */
@Component
class HelpCommand(
    private val commandsProvider: ObjectProvider<DiscordCommand>,
) : DiscordCommand {
    override val name = "help"
    override val usage = "/help"
    override val description = "명령어 목록 보기"
    override val adminOnly = false

    override fun definition(): SlashCommandData = Commands.slash(name, description)

    override fun execute(request: CommandRequest): CommandReply {
        val commands = commandsProvider.orderedStream().toList().sortedBy { it.name }
        val embed = EmbedBuilder().setTitle("🤖 지표봇 명령어")
        addField(embed, "📊 조회", commands.filterNot { it.adminOnly })
        addField(embed, "⚙️ 설정 (운영진만)", commands.filter { it.adminOnly })
        return CommandReply.embed(embed.build(), ephemeral = true)
    }

    private fun addField(
        embed: EmbedBuilder,
        title: String,
        commands: List<DiscordCommand>,
    ) {
        if (commands.isEmpty()) return
        embed.addField(title, commands.joinToString("\n") { "`${it.usage}` — ${it.description}" }, false)
    }
}
