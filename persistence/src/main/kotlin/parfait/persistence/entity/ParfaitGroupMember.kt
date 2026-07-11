package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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
        UniqueConstraint(
            name = "uk_parfait_group_member_group_nickname",
            columnNames = ["parfait_group_id", "group_nickname"],
        ),
    ],
)
open class ParfaitGroupMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parfait_group_id", nullable = false)
    open var parfaitGroup: ParfaitGroup,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    open var member: Member,
    @Column(name = "group_nickname", nullable = false, length = 50)
    open var groupNickname: String,
    @Column(name = "joined_at", nullable = false)
    open var joinedAt: LocalDateTime = LocalDateTime.now(),
)
