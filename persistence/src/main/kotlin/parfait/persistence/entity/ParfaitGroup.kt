package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "parfait_group",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_parfait_group_invite_code", columnNames = ["invite_code"]),
    ],
)
open class ParfaitGroup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,
    @Column(name = "name", nullable = false, length = 100)
    open var name: String,
    @Column(name = "invite_code", nullable = false, length = 50)
    open var inviteCode: String,
    @Column(name = "member_limit", nullable = false)
    open var memberLimit: Int,
    @Column(name = "created_at", nullable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    open var updatedAt: LocalDateTime = LocalDateTime.now(),
)
