package parfait.external.discord.command.signup

import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component
import parfait.core.report.domain.ReportContent
import parfait.core.report.domain.ReportPeriod
import parfait.core.stats.port.`in`.GetSignupStatsComparisonUseCase
import parfait.external.discord.command.CommandReply
import parfait.external.discord.command.CommandRequest
import parfait.external.discord.command.DiscordCommand
import parfait.external.discord.report.ReportEmbedRenderer
import java.time.Clock
import java.time.LocalDateTime

@Component
class SignupCommand(
    private val getSignupStatsComparisonUseCase: GetSignupStatsComparisonUseCase,
    private val renderer: ReportEmbedRenderer,
    private val clock: Clock,
) : DiscordCommand {
    override val name = "가입자"
    override val usage = "/가입자 [기간]"
    override val description = "가입·탈퇴·누적 가입자 (예: /가입자 7일)"
    override val adminOnly = false

    override fun definition(): SlashCommandData =
        Commands.slash(name, description).addOptions(
            OptionData(OptionType.STRING, OPTION_PERIOD, "조회 기간 (기본 7일)", false)
                .addChoice("1일", "1")
                .addChoice("3일", "3")
                .addChoice("7일", "7")
                .addChoice("30일", "30"),
        )

    override fun execute(request: CommandRequest): CommandReply {
        val days = (request.options[OPTION_PERIOD] ?: DEFAULT_DAYS).toIntOrNull()
        if (days == null || days !in SUPPORTED_DAYS) {
            return CommandReply.text("지원하지 않는 기간입니다. 1일·3일·7일·30일 중에서 골라 주세요.", ephemeral = true)
        }
        val now = LocalDateTime.now(clock)
        val period = ReportPeriod(now.minusDays(days.toLong()), now)
        val comparison = getSignupStatsComparisonUseCase.getComparison(period.from, period.to)
        val content = ReportContent(period, comparison.current, comparison.growth)
        return CommandReply.embed(renderer.render(content), ephemeral = true)
    }

    companion object {
        private const val OPTION_PERIOD = "기간"
        private const val DEFAULT_DAYS = "7"
        private val SUPPORTED_DAYS = setOf(1, 3, 7, 30)
    }
}
