package parfait.external.discord.command.reportnow

import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.springframework.stereotype.Component
import parfait.core.report.port.`in`.BuildReportNowUseCase
import parfait.external.discord.command.CommandReply
import parfait.external.discord.command.CommandRequest
import parfait.external.discord.command.DiscordCommand
import parfait.external.discord.report.ReportEmbedRenderer
import java.time.Clock
import java.time.LocalDateTime

/** 정기 전송과 별개로 지금 리포트를 만들어 채널에 응답한다. `last_sent_at` 은 바뀌지 않는다. */
@Component
class ReportNowCommand(
    private val buildReportNowUseCase: BuildReportNowUseCase,
    private val renderer: ReportEmbedRenderer,
    private val clock: Clock,
) : DiscordCommand {
    override val name = "리포트"
    override val usage = "/리포트 지금"
    override val description = "정기 리포트를 바로 생성"
    override val adminOnly = false

    override fun definition(): SlashCommandData =
        Commands.slash(name, description).addSubcommands(SubcommandData("지금", "정기 리포트를 바로 만들어 올립니다"))

    override fun execute(request: CommandRequest): CommandReply =
        CommandReply.embed(renderer.render(buildReportNowUseCase.buildNow(LocalDateTime.now(clock))), ephemeral = false)
}
