package parfait.core.notice.domain

import parfait.core.exception.BusinessException
import parfait.core.notice.exception.NoticeErrorCode
import java.time.LocalDate
import java.time.LocalDateTime

data class Notice(
    val type: NoticeType,
    val platform: NoticePlatform,
    val version: String?,
    val revisionTarget: NoticeRevisionTarget?,
    val title: String,
    val content: String,
    val publishedDate: LocalDate,
    val createdAt: LocalDateTime,
    val id: Long? = null,
) {
    fun revise(
        content: String?,
        version: String?,
        revisionTarget: NoticeRevisionTarget?,
    ): Notice {
        if (content == null && version == null && revisionTarget == null) {
            throw BusinessException(NoticeErrorCode.INVALID_NOTICE)
        }
        val newVersion = version ?: this.version
        val newTarget = revisionTarget ?: this.revisionTarget
        val newContent = content ?: this.content
        validate(type, newVersion, newTarget, newContent)
        return copy(
            version = newVersion,
            revisionTarget = newTarget,
            content = newContent,
            title = buildTitle(type, newVersion, newTarget),
        )
    }

    companion object {
        private val VERSION_REGEX = Regex("^\\d{1,5}\\.\\d{1,5}\\.\\d{1,5}\$")

        fun create(
            type: NoticeType,
            platform: NoticePlatform,
            version: String?,
            revisionTarget: NoticeRevisionTarget?,
            content: String,
            publishedDate: LocalDate,
            createdAt: LocalDateTime,
        ): Notice {
            validate(type, version, revisionTarget, content)
            return Notice(
                type = type,
                platform = platform,
                version = version,
                revisionTarget = revisionTarget,
                title = buildTitle(type, version, revisionTarget),
                content = content,
                publishedDate = publishedDate,
                createdAt = createdAt,
            )
        }

        private fun validate(
            type: NoticeType,
            version: String?,
            revisionTarget: NoticeRevisionTarget?,
            content: String,
        ) {
            val valid =
                content.isNotBlank() &&
                    when (type) {
                        NoticeType.UPDATE -> version != null && VERSION_REGEX.matches(version) && revisionTarget == null
                        NoticeType.GUIDE -> revisionTarget != null && version == null
                    }
            if (!valid) throw BusinessException(NoticeErrorCode.INVALID_NOTICE)
        }

        private fun buildTitle(
            type: NoticeType,
            version: String?,
            revisionTarget: NoticeRevisionTarget?,
        ): String =
            when (type) {
                NoticeType.UPDATE -> "파르페 v$version 업데이트 안내"
                NoticeType.GUIDE ->
                    when (revisionTarget) {
                        NoticeRevisionTarget.BOTH -> "이용약관 및 개인정보처리방침 개정 안내"
                        NoticeRevisionTarget.TERMS -> "이용약관 개정 안내"
                        NoticeRevisionTarget.PRIVACY -> "개인정보처리방침 개정 안내"
                        null -> throw BusinessException(NoticeErrorCode.INVALID_NOTICE)
                    }
            }
    }
}
