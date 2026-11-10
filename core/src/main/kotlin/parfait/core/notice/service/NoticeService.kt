package parfait.core.notice.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.exception.NoticeErrorCode
import parfait.core.notice.port.`in`.GetNoticesUseCase
import parfait.core.notice.port.`in`.RegisterNoticeCommand
import parfait.core.notice.port.`in`.RegisterNoticeUseCase
import parfait.core.notice.port.`in`.UpdateNoticeCommand
import parfait.core.notice.port.`in`.UpdateNoticeUseCase
import parfait.core.notice.port.out.NoticeQueryPort
import parfait.core.notice.port.out.NoticeSavePort
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class NoticeService(
    private val noticeQueryPort: NoticeQueryPort,
    private val noticeSavePort: NoticeSavePort,
    private val clock: Clock,
) : GetNoticesUseCase,
    RegisterNoticeUseCase,
    UpdateNoticeUseCase {
    @Transactional(readOnly = true)
    override fun getNotices(platform: NoticePlatform): List<Notice> {
        if (platform == NoticePlatform.COMMON) {
            throw BusinessException(NoticeErrorCode.INVALID_NOTICE_PLATFORM)
        }
        return noticeQueryPort.findVisible(platform, LocalDate.now(clock))
    }

    @Transactional
    override fun register(command: RegisterNoticeCommand): Notice {
        val notice =
            Notice.create(
                type = command.type,
                platform = command.platform,
                version = command.version,
                revisionTarget = command.revisionTarget,
                content = command.content,
                publishedDate = command.publishedDate ?: LocalDate.now(clock),
                createdAt = LocalDateTime.now(clock),
            )
        return noticeSavePort.save(notice)
    }

    @Transactional
    override fun update(
        id: Long,
        command: UpdateNoticeCommand,
    ): Notice {
        val notice =
            noticeQueryPort.findById(id)
                ?: throw BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND)
        val revised = notice.revise(command.content, command.version, command.revisionTarget)
        return noticeSavePort.save(revised)
    }
}
