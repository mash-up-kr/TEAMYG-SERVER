package parfait.http.api.notice.dto

import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticeType
import java.time.LocalDate

data class NoticeResponse(
    val id: Long,
    val type: NoticeType,
    val title: String,
    val content: String,
    val publishedDate: LocalDate,
) {
    companion object {
        fun from(notice: Notice): NoticeResponse =
            NoticeResponse(
                id = requireNotNull(notice.id),
                type = notice.type,
                title = notice.title,
                content = notice.content,
                publishedDate = notice.publishedDate,
            )
    }
}
