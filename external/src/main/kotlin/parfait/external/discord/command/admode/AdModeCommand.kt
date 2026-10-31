package parfait.external.discord.command.admode

import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.springframework.stereotype.Component
import parfait.core.report.domain.ReportStatus
import parfait.core.report.port.`in`.ChangeAdModeUseCase
import parfait.core.report.port.`in`.GetReportStatusUseCase
import parfait.external.discord.command.CommandReply
import parfait.external.discord.command.CommandRequest
import parfait.external.discord.command.DiscordCommand
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class AdModeCommand(
    private val changeAdModeUseCase: ChangeAdModeUseCase,
    private val getReportStatusUseCase: GetReportStatusUseCase,
    private val clock: Clock,
) : DiscordCommand {
    override val name = "광고모드"
    override val usage = "/광고모드 켜기|끄기|상태"
    override val description = "리포트 주기 전환 (매일 ↔ 3일)"
    override val adminOnly = true

    override fun definition(): SlashCommandData =
        Commands.slash(name, description).addSubcommands(
            SubcommandData("켜기", "광고모드를 켭니다 (리포트를 매일 전송)"),
            SubcommandData("끄기", "광고모드를 끕니다 (리포트를 주기마다 전송)"),
            SubcommandData("상태", "현재 모드와 다음 리포트 날짜를 봅니다"),
        )

    override fun execute(request: CommandRequest): CommandReply {
        val now = LocalDateTime.now(clock)
        return when (request.subcommand) {
            "켜기" -> changed("광고모드를 켰습니다.", changeAdModeUseCase.changeAdMode(true, now))
            "끄기" -> changed("광고모드를 껐습니다.", changeAdModeUseCase.changeAdMode(false, now))
            "상태" -> CommandReply.text(describe(getReportStatusUseCase.getStatus(now)), ephemeral = true)
            else -> CommandReply.text("켜기, 끄기, 상태 중에서 골라 주세요.", ephemeral = true)
        }
    }

    // 모드 변경은 팀이 알 수 있게 채널에 공개한다.
    private fun changed(
        headline: String,
        status: ReportStatus,
    ) = CommandReply.text("$headline ${describe(status)}", ephemeral = false)

    private fun describe(status: ReportStatus): String {
        val mode = if (status.adMode) "ON" else "OFF"
        val interval = if (status.adMode) "매일" else "${status.intervalDays}일마다"
        val last = status.lastSentAt?.format(LAST_SENT_FORMAT) ?: "없음"
        val next = status.nextReportDate.format(NEXT_DATE_FORMAT)
        return "광고모드: $mode · 주기: $interval · 마지막 전송: $last · 다음 리포트: $next"
    }

    companion object {
        private val LAST_SENT_FORMAT = DateTimeFormatter.ofPattern("M/d HH:mm")
        private val NEXT_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d")
    }
}
