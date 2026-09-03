package parfait.core.notification.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import parfait.core.notification.domain.NotificationMessageFactory
import parfait.core.notification.domain.ReminderType
import parfait.core.notification.port.`in`.ReminderSendOutcome
import parfait.core.notification.port.`in`.SendDailyReminderUseCase
import parfait.core.notification.port.out.DeviceTokenDeletePort
import parfait.core.notification.port.out.NotificationSenderPort
import parfait.core.notification.port.out.ReminderTargetQueryPort
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class DailyReminderSender(
    private val targetQueryPort: ReminderTargetQueryPort,
    private val senderPort: NotificationSenderPort,
    private val deviceTokenDeletePort: DeviceTokenDeletePort,
    private val messageFactory: NotificationMessageFactory,
    // 테스트에서 0 을 넘겨 슬립을 건너뛴다. @Value/@ConfigurationProperties 아님 — 기본값 상수.
    private val chunkDelayMillis: Long = CHUNK_DELAY_MS,
) : SendDailyReminderUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(
        type: ReminderType,
        now: LocalDateTime,
    ): ReminderSendOutcome {
        if (type == ReminderType.EVENING && !now.toLocalTime().isBefore(EVENING_CUTOFF)) {
            log.warn("P-03 리마인드 스킵: 배치 시작 {} 이 {} 이후", now, EVENING_CUTOFF)
            return ReminderSendOutcome(skipped = true, targeted = 0, sent = 0, failed = 0, deadTokensDeleted = 0)
        }

        val tokens = targetQueryPort.findActiveGroupMemberDeviceTokens()
        if (tokens.isEmpty()) {
            log.info("리마인드 발송 - type={} 대상=0 (발송 대상 없음)", type)
            return ReminderSendOutcome(skipped = false, targeted = 0, sent = 0, failed = 0, deadTokensDeleted = 0)
        }

        val message = messageFactory.dailyReminder(type)
        val dead = mutableListOf<String>()
        var sent = 0
        var failed = 0

        val chunks = tokens.chunked(CHUNK_SIZE)
        chunks.forEachIndexed { index, chunk ->
            val r = senderPort.sendMulticast(chunk, message)
            sent += r.successCount
            failed += r.failureCount
            dead += r.deadTokens()
            if (chunkDelayMillis > 0 && index < chunks.lastIndex) Thread.sleep(chunkDelayMillis)
        }

        val deleted =
            dead
                .distinct()
                .chunked(DELETE_CHUNK)
                .sumOf { deviceTokenDeletePort.deleteByTokenIn(it) }

        log.info(
            "리마인드 발송 - type={} 대상={} 성공={} 실패={} 죽은토큰삭제={}",
            type,
            tokens.size,
            sent,
            failed,
            deleted,
        )
        return ReminderSendOutcome(
            skipped = false,
            targeted = tokens.size,
            sent = sent,
            failed = failed,
            deadTokensDeleted = deleted,
        )
    }

    companion object {
        const val CHUNK_SIZE = 500
        const val CHUNK_DELAY_MS = 200L
        const val DELETE_CHUNK = 5000
        val EVENING_CUTOFF: LocalTime = LocalTime.of(21, 0)
    }
}
