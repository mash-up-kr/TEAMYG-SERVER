package parfait.external.notification

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import parfait.core.notification.port.`in`.PurgeNotificationOutboxUseCase
import java.time.Duration

/** 종료 상태(SENT/FAILED) 행을 하루 1회 걷어낸다. MySQL 에 부분 인덱스가 없는 것의 대응책. */
@Component
class OutboxRetentionSweeper(
    private val useCase: PurgeNotificationOutboxUseCase,
    @Value("\${notification.outbox.retention-days:7}") private val retentionDays: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${notification.outbox.purge-cron:0 0 4 * * *}", zone = "Asia/Seoul")
    fun sweep() {
        runCatching { useCase.purgeTerminal(Duration.ofDays(retentionDays)) }
            .onSuccess { log.info("notification_outbox 보관 정리: {}행 삭제", it) }
            .onFailure { log.error("notification_outbox 보관 정리 실패", it) }
    }
}
