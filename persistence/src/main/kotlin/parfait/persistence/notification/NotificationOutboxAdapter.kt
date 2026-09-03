package parfait.persistence.notification

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.notification.domain.NotificationOutbox
import parfait.core.notification.domain.OutboxStatus
import parfait.core.notification.domain.ToppingPlacedPayload
import parfait.core.notification.port.out.NotificationOutboxAppendPort
import parfait.core.notification.port.out.NotificationOutboxPollPort
import parfait.core.notification.port.out.NotificationOutboxPurgePort
import parfait.persistence.entity.NotificationOutboxEntity
import parfait.persistence.repository.NotificationOutboxRepository
import java.time.LocalDateTime

@Component
@Transactional
class NotificationOutboxAdapter(
    private val repository: NotificationOutboxRepository,
) : NotificationOutboxAppendPort,
    NotificationOutboxPollPort,
    NotificationOutboxPurgePort {
    private companion object {
        const val PURGE_CHUNK = 5_000
    }

    private val objectMapper: JsonMapper =
        JsonMapper
            .builder()
            .addModule(kotlinModule())
            .addModule(JavaTimeModule())
            .build()

    /**
     * place() 트랜잭션 안에서 수신자당 1행을 저장한다. 생산자 멱등:
     * 넘어온 dedup_key 중 이미 큐잉된 것을 먼저 조회해 걸러내고 새 행만 INSERT 하므로
     * 정상 경로에서 UNIQUE(uk_notification_outbox_dedup) 위반이 발생하지 않는다.
     * 호출자 트랜잭션을 그대로 유지한다 — 별도 propagation 을 도입하지 않으며,
     * Transactional Outbox 의 "발송 의도를 토핑 저장과 같은 트랜잭션에 기록" 불변식을 지킨다.
     *
     * 레이스: 같은 toppingId+receiver 로 place() 가 동시에 2회 실행되면 두 트랜잭션이
     * 같은 dedup_key 를 각자 "신규"로 판단해 둘 다 INSERT 를 시도할 수 있다. 이때 나중에
     * 커밋하는 쪽이 DataIntegrityViolationException 을 받고 place() 전체가 롤백되며,
     * 상위(토핑 등록 재시도)가 다시 처리한다 — at-least-once(스펙 §2 소비자 멱등이 허용).
     */
    override fun saveAll(messages: List<NotificationOutbox>) {
        if (messages.isEmpty()) return
        val alreadyQueued =
            repository
                .findAllByDedupKeyIn(messages.map { it.dedupKey })
                .mapTo(mutableSetOf()) { it.dedupKey }
        val fresh = messages.filterNot { it.dedupKey in alreadyQueued }
        if (fresh.isEmpty()) return
        repository.saveAll(fresh.map { it.toEntity() })
    }

    override fun claimBatch(
        limit: Int,
        now: LocalDateTime,
    ): List<NotificationOutbox> = repository.claimBatch(now, limit).map { it.toDomain() }

    override fun markSent(
        id: Long,
        now: LocalDateTime,
        note: String?,
    ) {
        val e = repository.findById(id).orElseThrow()
        e.status = OutboxStatus.SENT
        e.sentAt = now
        e.lastError = note
    }

    override fun markRetry(
        id: Long,
        attempts: Int,
        scheduledAt: LocalDateTime,
        error: String,
    ) {
        val e = repository.findById(id).orElseThrow()
        e.attempts = attempts
        e.scheduledAt = scheduledAt
        e.lastError = error
    }

    override fun markFailed(
        id: Long,
        error: String,
    ) {
        val e = repository.findById(id).orElseThrow()
        e.status = OutboxStatus.FAILED
        e.lastError = error
    }

    override fun deleteTerminalBefore(cutoff: LocalDateTime): Int {
        var total = 0
        while (true) {
            val n = repository.deleteTerminalBefore(cutoff, PURGE_CHUNK)
            total += n
            if (n < PURGE_CHUNK) break
        }
        return total
    }

    private fun NotificationOutbox.toEntity() =
        NotificationOutboxEntity(
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            eventType = eventType,
            receiverMemberId = receiverMemberId,
            payload = objectMapper.writeValueAsString(payload),
            dedupKey = dedupKey,
            status = status,
            attempts = attempts,
            scheduledAt = scheduledAt,
            lastError = lastError,
            createdAt = createdAt,
            sentAt = sentAt,
            id = id,
        )

    private fun NotificationOutboxEntity.toDomain(): NotificationOutbox =
        NotificationOutbox.reconstitute(
            id = id!!,
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            eventType = eventType,
            receiverMemberId = receiverMemberId,
            payload = objectMapper.readValue<ToppingPlacedPayload>(payload),
            dedupKey = dedupKey,
            status = status,
            attempts = attempts,
            scheduledAt = scheduledAt,
            lastError = lastError,
            createdAt = createdAt,
            sentAt = sentAt,
        )
}
