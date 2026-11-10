package parfait.http.api.notice.dto

import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.domain.NoticeRevisionTarget
import parfait.core.notice.domain.NoticeType
import java.time.LocalDate

data class AdminNoticeResponse(
    val id: Long,
    val type: NoticeType,
    val platform: NoticePlatform,
    val version: String?,
    val revisionTarget: NoticeRevisionTarget?,
    val title: String,
    val content: String,
    val publishedDate: LocalDate,
) {
    companion object {
        fun from(notice: Notice): AdminNoticeResponse =
            AdminNoticeResponse(
                id = requireNotNull(notice.id),
                type = notice.type,
                platform = notice.platform,
                version = notice.version,
                revisionTarget = notice.revisionTarget,
                title = notice.title,
                content = notice.content,
                publishedDate = notice.publishedDate,
            )
    }
}
