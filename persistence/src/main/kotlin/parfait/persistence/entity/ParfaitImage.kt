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
import java.time.LocalDateTime

@Entity
@Table(name = "parfait_image")
open class ParfaitImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parfait_id", nullable = false)
    open var parfait: Parfait,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_meta_id", nullable = false)
    open var imageMeta: ImageMeta,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by_member_id", nullable = false)
    open var updatedBy: Member,
    @Column(name = "image_url", nullable = false, length = 2048)
    open var imageUrl: String,
    @Column(name = "width", nullable = false)
    open var width: Int,
    @Column(name = "height", nullable = false)
    open var height: Int,
    @Column(name = "rotation", nullable = false)
    open var rotation: Double = 0.0,
    @Column(name = "created_at", nullable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    open var updatedAt: LocalDateTime = LocalDateTime.now(),
)
