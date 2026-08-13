package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "parfait",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_parfait_group_parfait_date",
            columnNames = ["parfait_group_id", "parfait_date"],
        ),
    ],
)
class Parfait(
    @Column(name = "parfait_group_id", nullable = false)
    var parfaitGroupId: Long,
    @Column(name = "parfait_date", nullable = false)
    var parfaitDate: LocalDate,
    @Column(name = "status", nullable = false, length = 50)
    var status: String,
    @Column(name = "background_type", length = 50)
    var backgroundType: String?,
    @Column(name = "background_value", length = 2048)
    var backgroundValue: String?,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
