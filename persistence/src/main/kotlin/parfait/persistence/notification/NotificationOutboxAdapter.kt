package parfait.persistence.notification

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.dao.DataIntegrityViolationException
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

    override fun saveAll(messages: List<NotificationOutbox>) {
        for (m in messages) {
            try {
                repository.save(m.toEntity())
                repository.flush() // dedup 충돌을 이 지점에서 표면화 (다음 건은 계속 저장)
            } catch (e: DataIntegrityViolationException) {
                // 생산자 멱등: 같은 dedup_key 는 무시
            }
        }
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
