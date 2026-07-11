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
    name = "tos_agreement",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_tos_agreement_member_tos", columnNames = ["member_id", "tos_id"]),
    ],
)
class TosAgreement(
    @Column(name = "member_id", nullable = false)
    var memberId: Long,
    @Column(name = "tos_id", nullable = false)
    var tosId: Long,
    @Column(name = "agreed_at", nullable = false)
    var agreedAt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
