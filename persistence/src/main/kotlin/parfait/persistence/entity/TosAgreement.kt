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
    name = "tos_agreement",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_tos_agreement_member_tos", columnNames = ["member_id", "tos_id"]),
    ],
)
open class TosAgreement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    open var member: Member,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tos_id", nullable = false)
    open var tos: Tos,
    @Column(name = "agreed_at", nullable = false)
    open var agreedAt: LocalDateTime = LocalDateTime.now(),
)
