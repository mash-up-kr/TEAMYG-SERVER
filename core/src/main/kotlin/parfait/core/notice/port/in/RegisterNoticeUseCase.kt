@file:Suppress("ktlint:standard:package-name")

package parfait.core.notice.port.`in`

import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.domain.NoticeRevisionTarget
import parfait.core.notice.domain.NoticeType
import java.time.LocalDate

data class RegisterNoticeCommand(
    val type: NoticeType,
    val platform: NoticePlatform,
    val version: String?,
    val revisionTarget: NoticeRevisionTarget?,
    val content: String,
    val publishedDate: LocalDate?,
)

interface RegisterNoticeUseCase {
    fun register(command: RegisterNoticeCommand): Notice
}
