package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "parfait_text", indexes = [Index(name = "idx_parfait_text_parfait_id", columnList = "parfait_id")])
class ParfaitText(
    @Column(name = "parfait_id", nullable = false)
    var parfaitId: Long,
    @Column(name = "placed_by_group_member_id", nullable = false)
    var placedByGroupMemberId: Long,
    @Column(name = "text", nullable = false, length = 2000)
    var text: String,
    @Column(name = "position_x", nullable = false)
    var positionX: Double,
    @Column(name = "position_y", nullable = false)
    var positionY: Double,
    @Column(name = "position_z", nullable = false)
    var positionZ: Int,
    @Column(name = "scale", nullable = false)
    var scale: Double,
    @Column(name = "rotation", nullable = false)
    var rotation: Double,
    @Column(name = "color", nullable = false, length = 20)
    var color: String,
    @Column(name = "font_size", nullable = false)
    var fontSize: Double,
    @Column(name = "is_fill", nullable = false)
    var isFill: Boolean = false,
    @Column(name = "is_stroke", nullable = false)
    var isStroke: Boolean = false,
    @Column(name = "text_align", nullable = false, length = 20)
    var textAlign: String = "CENTER",
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
