package parfait.http.api.notice.dto

import jakarta.validation.constraints.NotBlank
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.domain.NoticeRevisionTarget
import parfait.core.notice.domain.NoticeType
import parfait.core.notice.port.`in`.RegisterNoticeCommand
import java.time.LocalDate

data class RegisterNoticeRequest(
    val type: NoticeType,
    val platform: NoticePlatform,
    val version: String? = null,
    val revisionTarget: NoticeRevisionTarget? = null,
    @field:NotBlank val content: String,
    val publishedDate: LocalDate? = null,
) {
    fun toCommand(): RegisterNoticeCommand =
        RegisterNoticeCommand(
            type = type,
            platform = platform,
            version = version,
            revisionTarget = revisionTarget,
            content = content,
            publishedDate = publishedDate,
        )
}
