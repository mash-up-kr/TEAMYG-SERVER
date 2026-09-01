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
import java.time.LocalDateTime

@Entity
@Table(
    name = "device_token",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_device_token_token", columnNames = ["token"]),
    ],
)
class DeviceToken(
    @Column(name = "member_id", nullable = false)
    var memberId: Long,
    @Column(name = "token", nullable = false, length = 512)
    var token: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    var platform: DevicePlatform,
    @Column(name = "session_id", length = 64)
    var sessionId: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
