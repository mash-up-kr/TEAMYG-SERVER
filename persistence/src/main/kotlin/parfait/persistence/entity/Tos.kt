package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
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
        UniqueConstraint(name = "uk_tos_version", columnNames = ["version"]),
    ],
)
open class Tos(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    open var id: Long? = null,
    @Column(name = "version", nullable = false, length = 30)
    open var version: String,
    @Column(name = "title", nullable = false, length = 100)
    open var title: String,
    @Lob
    @Column(name = "content", nullable = false)
    open var content: String,
    @Column(name = "required", nullable = false)
    open var required: Boolean = true,
    @Column(name = "published_at", nullable = false)
    open var publishedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "created_at", nullable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),
)
