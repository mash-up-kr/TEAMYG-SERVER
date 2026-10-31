package parfait.external.discord.report

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import parfait.core.report.domain.ReportContent
import parfait.core.report.port.out.ReportSendPort

/**
 * 봇 토큰이 없는 환경(로컬·테스트)에서 `ReportService` 가 주입받을 [ReportSendPort] 를 채우는 자리 표시용 구현.
 * 스케줄러는 `report.enabled=true` 일 때만 켜지므로 정상 환경에서는 호출되지 않는다.
 * 토큰 없이 리포트를 켠 설정 실수는 조용히 넘기지 않고 예외로 드러낸다(→ ERROR 로그 → #errors).
 */
@Component
@ConditionalOnExpression("'\${discord.bot.token:}'.isEmpty()")
class DisabledReportSender : ReportSendPort {
    override fun send(content: ReportContent): Unit =
        throw IllegalStateException("discord.bot.token 이 설정되지 않아 리포트를 보낼 수 없습니다")
}
