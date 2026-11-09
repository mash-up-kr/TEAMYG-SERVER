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
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.domain.NoticeRevisionTarget
import parfait.core.notice.domain.NoticeType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "notice")
class Notice(
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    var type: NoticeType,
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    var platform: NoticePlatform,
    @Column(name = "version", length = 20)
    var version: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "revision_target", length = 20)
    var revisionTarget: NoticeRevisionTarget?,
    @Column(name = "title", nullable = false, length = 255)
    var title: String,
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    var content: String,
    @Column(name = "published_date", nullable = false)
    var publishedDate: LocalDate,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
