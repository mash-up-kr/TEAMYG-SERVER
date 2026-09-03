package parfait.external.notification

import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import parfait.core.notification.port.`in`.ProcessNotificationOutboxUseCase
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Outbox 폴링 워커. 배치 스케줄러가 아니라 준실시간 폴링 워커 — 주기는 튜닝값.
 * @Scheduled 틱과 wakeUp() 이 같은 단일 스레드 실행기에 drain 을 제출한다.
 * scheduled 플래그로 중복 제출을 합친다.
 */
@Component
class OutboxPollingWorker(
    private val useCase: ProcessNotificationOutboxUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scheduled = AtomicBoolean(false)
    private val executor =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "outbox-poller").apply { isDaemon = true }
        }

    private companion object {
        const val MAX_ITERATIONS = 20
    }

    @Scheduled(fixedDelayString = "\${notification.outbox.poll-interval-ms:2000}")
    fun tick() {
        submit()
    }

    fun wakeUp() {
        submit()
    }

    private fun submit() {
        if (scheduled.compareAndSet(false, true)) {
            // @PreDestroy shutdown() 이후의 tick()/wakeUp() 은 RejectedExecutionException 을 던진다.
            // 실패 시 플래그를 되돌려 다음 제출을 막지 않고, 예외를 AFTER_COMMIT 동기화 밖으로 전파하지 않는다.
            runCatching { executor.execute(::drain) }
                .onFailure { scheduled.set(false) }
        }
    }

    internal fun drain() {
        scheduled.set(false)
        var iterations = 0
        var claimedTotal = 0
        var sentTotal = 0
        var failedTotal = 0
        var retriedTotal = 0
        var cancelledTotal = 0
        val cancelledByReasonTotal = mutableMapOf<String, Int>()

        while (iterations < MAX_ITERATIONS) {
            iterations++
            val outcome =
                runCatching { useCase.processDueBatch() }
                    .getOrElse {
                        log.error("notification_outbox 폴링 배치 실패", it)
                        return
                    }
            claimedTotal += outcome.claimed
            sentTotal += outcome.sent
            failedTotal += outcome.failed
            retriedTotal += outcome.retried
            cancelledTotal += outcome.cancelled
            outcome.cancelledByReason.forEach { (k, v) -> cancelledByReasonTotal.merge(k, v, Int::plus) }
            if (outcome.claimed == 0) break
        }

        if (claimedTotal > 0) {
            log.info(
                "outbox drain 완료 - iterations={} claimed={} sent={} cancelled={} retried={} failed={} byReason={}",
                iterations,
                claimedTotal,
                sentTotal,
                cancelledTotal,
                retriedTotal,
                failedTotal,
                cancelledByReasonTotal,
            )
        }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdown()
    }
}
