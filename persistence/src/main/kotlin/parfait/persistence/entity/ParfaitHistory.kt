package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "parfait_history")
open class ParfaitHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parfait_id", nullable = false)
    open var parfait: Parfait,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parfait_image_id")
    open var parfaitImage: ParfaitImage? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    open var member: Member,
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    open var actionType: ParfaitImageActionType,
    @Lob
    @Column(name = "history_json", nullable = false, columnDefinition = "json")
    open var historyJson: String,
    @Column(name = "created_at", nullable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),
)
