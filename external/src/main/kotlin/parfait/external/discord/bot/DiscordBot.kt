package parfait.external.discord.bot

import jakarta.annotation.PreDestroy
import net.dv8tion.jda.api.JDA
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import parfait.core.report.domain.ReportContent
import parfait.core.report.port.out.ReportSendPort
import parfait.external.discord.report.DisabledReportSender
import parfait.external.discord.report.ReportEmbedRenderer
import java.util.concurrent.TimeUnit

/**
 * 디스코드 봇 연결과 리포트 채널 전송. 토큰이 비어 있으면(로컬·테스트) 이 빈은 만들어지지 않고
 * [DisabledReportSender] 가 대신 들어간다.
 *
 * 명령어 처리 쪽 빈(CommandRouter → ... → ReportService)이 이 빈을 다시 의존하므로 생성자 순환을 피하려고
 * 리스너는 [ObjectProvider] 로 받고, JDA 연결은 모든 싱글톤이 만들어진 뒤([ApplicationReadyEvent])에 한다.
 * 연결 실패가 앱 기동을 막지 않도록 [start] 의 예외는 삼키고, 전송 시점에 [send] 가 예외를 던져
 * [parfait.core.report.service.ReportService] 의 재시도·ERROR 로그 경로를 타게 한다.
 * 이 클래스의 실패 로그는 WARN 이하로 둔다 — ERROR 는 DiscordErrorAppender 가 웹훅으로 보낸다.
 */
@Component
@ConditionalOnExpression("!'\${discord.bot.token:}'.isEmpty()")
class DiscordBot(
    @Value("\${discord.bot.token}") private val token: String,
    @Value("\${discord.bot.report-channel-id:}") private val reportChannelId: String,
    private val renderer: ReportEmbedRenderer,
    private val jdaFactory: ObjectProvider<JdaFactory>,
    private val commandRouter: ObjectProvider<CommandRouter>,
    private val commandRegistrar: ObjectProvider<CommandRegistrar>,
) : ReportSendPort {
    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var jda: JDA? = null

    @Synchronized
    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        if (jda != null) return
        jda =
            runCatching {
                val factory = jdaFactory.getIfAvailable { DefaultJdaFactory }
                factory.create(token, listOf(commandRouter.getObject(), commandRegistrar.getObject()))
            }.onFailure { log.warn("디스코드 봇 연결 실패 — 리포트 전송이 동작하지 않는다", it) }
                .getOrNull()
    }

    @PreDestroy
    fun stop() {
        jda?.shutdown()
    }

    override fun send(content: ReportContent) {
        check(reportChannelId.isNotBlank()) { "discord.bot.report-channel-id 가 설정되지 않았습니다" }
        val bot = checkNotNull(jda) { "디스코드 봇이 연결되지 않았습니다" }
        // awaitReady() 는 타임아웃이 없어 스케줄러 스레드를 막을 수 있으므로 즉시 실패시킨다.
        // JDA 가 자동 재연결하고 ReportService 가 1회 재시도하므로 즉시 실패해도 된다.
        check(bot.status == JDA.Status.CONNECTED) { "디스코드 봇이 아직 연결되지 않았습니다" }
        val channel =
            checkNotNull(bot.getTextChannelById(reportChannelId)) {
                "리포트 채널을 찾을 수 없습니다: $reportChannelId"
            }
        channel.sendMessageEmbeds(renderer.render(content)).timeout(10, TimeUnit.SECONDS).complete()
    }
}
