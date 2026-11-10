@file:Suppress("ktlint:standard:package-name")

package parfait.core.notice.port.`in`

import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticeRevisionTarget

data class UpdateNoticeCommand(
    val content: String?,
    val version: String?,
    val revisionTarget: NoticeRevisionTarget?,
)

interface UpdateNoticeUseCase {
    fun update(
        id: Long,
        command: UpdateNoticeCommand,
    ): Notice
}
