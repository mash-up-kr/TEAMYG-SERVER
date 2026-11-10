package parfait.http.api.notice.dto

import parfait.core.notice.domain.NoticeRevisionTarget
import parfait.core.notice.port.`in`.UpdateNoticeCommand

data class UpdateNoticeRequest(
    val content: String? = null,
    val version: String? = null,
    val revisionTarget: NoticeRevisionTarget? = null,
) {
    fun toCommand(): UpdateNoticeCommand =
        UpdateNoticeCommand(content = content, version = version, revisionTarget = revisionTarget)
}
