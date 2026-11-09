package parfait.core.notice.port.out

import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticePlatform
import java.time.LocalDate

interface NoticeQueryPort {
    /** `platform IN (platform, COMMON)` 이고 `publishedDate <= today` 인 공지를 `publishedDate DESC, id DESC`로 반환한다. */
    fun findVisible(
        platform: NoticePlatform,
        today: LocalDate,
    ): List<Notice>

    fun findById(id: Long): Notice?
}
