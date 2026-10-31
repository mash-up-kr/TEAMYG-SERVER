package parfait.external.discord.bot

import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import parfait.external.discord.command.DiscordCommand

/**
 * 봇이 준비되면 우리 서버(guild)에만 슬래시 명령어를 등록한다 — 전역 등록과 달리 즉시 반영된다.
 * 기존에 등록된 명령어 중 코드에 없는 것은 이 호출로 제거된다.
 */
@Component
@ConditionalOnExpression("!'\${discord.bot.token:}'.isEmpty()")
class CommandRegistrar(
    private val commands: List<DiscordCommand>,
    @Value("\${discord.bot.guild-id:}") private val guildId: String,
) : ListenerAdapter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onReady(event: ReadyEvent) {
        if (guildId.isBlank()) {
            log.warn("discord.bot.guild-id 가 비어 있어 슬래시 명령어를 등록하지 않는다")
            return
        }
        val guild = event.jda.getGuildById(guildId)
        if (guild == null) {
            log.warn("길드({})를 찾을 수 없어(봇이 초대되지 않았거나 아직 로드되지 않음) 슬래시 명령어를 등록하지 않는다", guildId)
            return
        }
        guild
            .updateCommands()
            .addCommands(commands.map { it.definition() })
            .queue(
                { log.info("슬래시 명령어 {}개 등록 완료", it.size) },
                { log.warn("슬래시 명령어 등록 실패", it) },
            )
    }
}
