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
        var i = 0
        while (i++ < MAX_ITERATIONS) {
            val outcome =
                runCatching { useCase.processDueBatch() }
                    .getOrElse {
                        log.error("notification_outbox 폴링 배치 실패", it)
                        return
                    }
            if (outcome.claimed == 0) return
        }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdown()
    }
}
