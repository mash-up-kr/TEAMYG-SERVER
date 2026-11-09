package parfait.persistence.notice

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.port.out.NoticeQueryPort
import parfait.core.notice.port.out.NoticeSavePort
import parfait.persistence.repository.NoticeRepository
import java.time.LocalDate
import parfait.persistence.entity.Notice as NoticeEntity

@Component
@Transactional
class NoticeAdapter(
    private val noticeRepository: NoticeRepository,
) : NoticeQueryPort,
    NoticeSavePort {
    @Transactional(readOnly = true)
    override fun findVisible(
        platform: NoticePlatform,
        today: LocalDate,
    ): List<Notice> =
        noticeRepository
            .findByPlatformInAndPublishedDateLessThanEqualOrderByPublishedDateDescIdDesc(
                setOf(platform, NoticePlatform.COMMON),
                today,
            ).map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Notice? = noticeRepository.findById(id).orElse(null)?.toDomain()

    override fun save(notice: Notice): Notice = noticeRepository.save(notice.toEntity()).toDomain()

    private fun NoticeEntity.toDomain(): Notice =
        Notice(
            type = type,
            platform = platform,
            version = version,
            revisionTarget = revisionTarget,
            title = title,
            content = content,
            publishedDate = publishedDate,
            createdAt = createdAt,
            id = id,
        )

    private fun Notice.toEntity(): NoticeEntity =
        NoticeEntity(
            type = type,
            platform = platform,
            version = version,
            revisionTarget = revisionTarget,
            title = title,
            content = content,
            publishedDate = publishedDate,
            createdAt = createdAt,
            id = id,
        )
}
