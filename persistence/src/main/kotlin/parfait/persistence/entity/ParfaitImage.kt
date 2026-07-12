package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "parfait_image")
class ParfaitImage(
    @Column(name = "parfait_id", nullable = false)
    var parfaitId: Long,
    @Column(name = "image_meta_id", nullable = false)
    var imageMetaId: Long,
    @Column(name = "updated_by_member_id", nullable = false)
    var updatedByMemberId: Long,
    @Column(name = "image_url", nullable = false, length = 2048)
    var imageUrl: String,
    @Column(name = "width", nullable = false)
    var width: Int,
    @Column(name = "height", nullable = false)
    var height: Int,
    @Column(name = "rotation", nullable = false)
    var rotation: Double = 0.0,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
