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
    name = "parfait_image",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_parfait_image_parfait_image_meta",
            columnNames = ["parfait_id", "image_meta_id"],
        ),
    ],
)
class ParfaitImage(
    @Column(name = "parfait_id", nullable = false)
    var parfaitId: Long,
    @Column(name = "image_meta_id", nullable = false)
    var imageMetaId: Long,
    @Column(name = "placed_by_group_member_id", nullable = false)
    var placedByGroupMemberId: Long,
    @Column(name = "image_url", nullable = false, length = 2048)
    var imageUrl: String,
    @Column(name = "position_x", nullable = false)
    var positionX: Double,
    @Column(name = "position_y", nullable = false)
    var positionY: Double,
    @Column(name = "position_z", nullable = false)
    var positionZ: Int,
    @Column(name = "scale", nullable = false)
    var scale: Double,
    @Column(name = "rotation", nullable = false)
    var rotation: Double = 0.0,
    @Column(name = "border_type", nullable = false, length = 50)
    var borderType: String,
    @Column(name = "border_color", length = 20)
    var borderColor: String?,
    @Column(name = "border_width")
    var borderWidth: Double?,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
