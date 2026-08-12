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
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

@Entity
@SQLDelete(sql = "UPDATE member SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(
    name = "member",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_member_login_provider_provider_user_id",
            columnNames = ["login_provider", "provider_user_id"],
        ),
    ],
)
class Member(
    @Enumerated(EnumType.STRING)
    @Column(name = "login_provider", nullable = false, length = 255)
    var loginProvider: LoginProvider,
    @Column(name = "provider_user_id", nullable = false, length = 255)
    var providerUserId: String,
    @Column(name = "global_nickname", nullable = false, length = 255)
    var globalNickname: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
