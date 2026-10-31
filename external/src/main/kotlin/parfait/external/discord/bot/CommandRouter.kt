package parfait.external.discord.bot

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import parfait.external.discord.command.AdminRoleChecker
import parfait.external.discord.command.CommandDispatcher
import parfait.external.discord.command.CommandReply
import parfait.external.discord.command.CommandRequest

/**
 * JDA 슬래시 명령어 이벤트를 [CommandRequest] 로 바꿔 [CommandDispatcher] 에 넘기고 응답한다.
 * 명령 처리는 별도 이벤트 풀에서 동기로 실행되며 디스코드 3초 응답 제한이 있다. DB만 쓰는 현재 명령은
 * 허용 범위이고, 처리 시간이 긴 명령(Meta API 등 3단계)은 deferReply 도입이 필요하다.
 */
@Component
@ConditionalOnExpression("!'\${discord.bot.token:}'.isEmpty()")
class CommandRouter(
    private val dispatcher: CommandDispatcher,
    private val adminRoleChecker: AdminRoleChecker,
) : ListenerAdapter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val reply =
            try {
                // 길드 밖(DM)에서는 member 가 null 이므로 빈 목록 -> isAdmin=false 가 되어야 한다.
                val roleIds =
                    event.member
                        ?.roles
                        ?.map { it.id }
                        .orEmpty()
                val request =
                    CommandRequest(
                        subcommand = event.subcommandName,
                        options = event.options.associate { it.name to it.asString },
                        isAdmin = adminRoleChecker.isAdmin(roleIds),
                    )
                dispatcher.dispatch(event.name, request)
            } catch (e: Exception) {
                log.error("슬래시 명령어 처리 실패: /{}", event.name, e)
                CommandReply.text("처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", ephemeral = true)
            }

        val action = reply.embed?.let { event.replyEmbeds(it) } ?: event.reply(reply.text.orEmpty())
        action.setEphemeral(reply.ephemeral).queue(null) {
            log.warn("슬래시 명령어 응답 실패: /{} ({})", event.name, it.message)
        }
    }
}
