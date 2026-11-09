package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.core.notice.domain.NoticePlatform
import parfait.persistence.entity.Notice
import java.time.LocalDate

interface NoticeRepository : JpaRepository<Notice, Long> {
    fun findByPlatformInAndPublishedDateLessThanEqualOrderByPublishedDateDescIdDesc(
        platforms: Collection<NoticePlatform>,
        today: LocalDate,
    ): List<Notice>
}
