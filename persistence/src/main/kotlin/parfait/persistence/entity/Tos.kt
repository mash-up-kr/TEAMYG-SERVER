package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "tos",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_tos_type_version", columnNames = ["type", "version"]),
    ],
)
class Tos(
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 255)
    var type: TosType,
    @Column(name = "version", nullable = false, length = 255)
    var version: String,
    @Column(name = "title", nullable = false, length = 255)
    var title: String,
    @Lob
    @Column(name = "content", nullable = false)
    var content: String,
    @Column(name = "required", nullable = false)
    var required: Boolean = true,
    @Column(name = "published_at", nullable = false)
    var publishedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
