package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "parfait_group_reporting")
class ParfaitGroupReporting(
    @Column(name = "parfait_group_id", nullable = false)
    var parfaitGroupId: Long,
    @Column(name = "reporter_member_id", nullable = false)
    var reporterMemberId: Long,
    @Lob
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    var reason: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
