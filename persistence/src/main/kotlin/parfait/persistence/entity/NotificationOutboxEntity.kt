package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import parfait.core.notification.domain.OutboxStatus
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification_outbox",
    uniqueConstraints = [UniqueConstraint(name = "uk_notification_outbox_dedup", columnNames = ["dedup_key"])],
)
class NotificationOutboxEntity(
    @Column(name = "aggregate_type", nullable = false, length = 40)
    var aggregateType: String,
    @Column(name = "aggregate_id", nullable = false)
    var aggregateId: Long,
    @Column(name = "event_type", nullable = false, length = 60)
    var eventType: String,
    @Column(name = "receiver_member_id", nullable = false)
    var receiverMemberId: Long,
    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
    var payload: String,
    @Column(name = "dedup_key", nullable = false, length = 150)
    var dedupKey: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OutboxStatus,
    @Column(name = "attempts", nullable = false)
    var attempts: Int,
    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: LocalDateTime,
    @Column(name = "last_error", length = 500)
    var lastError: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
