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
    name = "parfait_group_member",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_parfait_group_member_group_member",
            columnNames = ["parfait_group_id", "member_id"],
        ),
    ],
)
class ParfaitGroupMember(
    @Column(name = "parfait_group_id", nullable = false)
    var parfaitGroupId: Long,
    @Column(name = "member_id", nullable = false)
    var memberId: Long,
    @Column(name = "group_nickname", nullable = false, length = 15)
    var groupNickname: String,
    @Column(name = "joined_at", nullable = false)
    var joinedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "left_at")
    var leftAt: LocalDateTime? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
