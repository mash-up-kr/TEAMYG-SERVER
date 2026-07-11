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
    name = "member",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_member_login_provider_provider_user_id",
            columnNames = ["login_provider", "provider_user_id"],
        ),
    ],
)
open class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "login_provider", nullable = false, length = 50)
    open var loginProvider: LoginProvider,
    @Column(name = "provider_user_id", nullable = false, length = 255)
    open var providerUserId: String,
    @Column(name = "global_nickname", nullable = false, length = 50)
    open var globalNickname: String,
    @Column(name = "created_at", nullable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    open var updatedAt: LocalDateTime = LocalDateTime.now(),
)
