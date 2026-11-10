package parfait.core.notice.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.domain.NoticeRevisionTarget
import parfait.core.notice.domain.NoticeType
import parfait.core.notice.exception.NoticeErrorCode
import parfait.core.notice.port.`in`.RegisterNoticeCommand
import parfait.core.notice.port.`in`.UpdateNoticeCommand
import parfait.core.notice.port.out.NoticeQueryPort
import parfait.core.notice.port.out.NoticeSavePort
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class NoticeServiceTest {
    private val queryPort = mockk<NoticeQueryPort>()
    private val savePort = mockk<NoticeSavePort>()
    private val zone = ZoneId.of("Asia/Seoul")
    private val clock = Clock.fixed(Instant.parse("2026-11-08T15:30:00Z"), zone)
    private val service = NoticeService(queryPort, savePort, clock)

    private val today = LocalDate.of(2026, 11, 9)

    private fun existing() =
        Notice(
            type = NoticeType.UPDATE,
            platform = NoticePlatform.IOS,
            version = "1.2.0",
            revisionTarget = null,
            title = "파르페 v1.2.0 업데이트 안내",
            content = "기존 내용",
            publishedDate = LocalDate.of(2026, 11, 1),
            createdAt = LocalDateTime.of(2026, 11, 1, 9, 0),
            id = 7,
        )

    @Test
    fun `getNotices는 오늘 날짜(KST)와 요청 플랫폼으로 포트를 조회한다`() {
        val notices = listOf(existing())
        every { queryPort.findVisible(NoticePlatform.IOS, today) } returns notices

        service.getNotices(NoticePlatform.IOS) shouldBe notices

        verify(exactly = 1) { queryPort.findVisible(NoticePlatform.IOS, today) }
    }

    @Test
    fun `getNotices는 COMMON 플랫폼이면 INVALID_NOTICE_PLATFORM`() {
        val e = shouldThrow<BusinessException> { service.getNotices(NoticePlatform.COMMON) }

        e.errorCode shouldBe NoticeErrorCode.INVALID_NOTICE_PLATFORM
        verify(exactly = 0) { queryPort.findVisible(any(), any()) }
    }

    @Test
    fun `register는 publishedDate가 없으면 오늘 날짜로 저장한다`() {
        val saved = slot<Notice>()
        every { savePort.save(capture(saved)) } answers { saved.captured.copy(id = 1) }

        val result =
            service.register(
                RegisterNoticeCommand(NoticeType.UPDATE, NoticePlatform.ANDROID, "1.3.0", null, "내용", null),
            )

        saved.captured.publishedDate shouldBe today
        saved.captured.createdAt shouldBe LocalDateTime.of(2026, 11, 9, 0, 30)
        saved.captured.title shouldBe "파르페 v1.3.0 업데이트 안내"
        result.id shouldBe 1
    }

    @Test
    fun `register는 publishedDate가 있으면 그 값을 쓴다`() {
        val saved = slot<Notice>()
        every { savePort.save(capture(saved)) } answers { saved.captured }
        val date = LocalDate.of(2026, 12, 1)

        service.register(
            RegisterNoticeCommand(NoticeType.GUIDE, NoticePlatform.COMMON, null, NoticeRevisionTarget.BOTH, "내용", date),
        )

        saved.captured.publishedDate shouldBe date
    }

    @Test
    fun `register는 불변식을 어기면 저장하지 않는다`() {
        val e =
            shouldThrow<BusinessException> {
                service.register(RegisterNoticeCommand(NoticeType.UPDATE, NoticePlatform.IOS, null, null, "내용", null))
            }

        e.errorCode shouldBe NoticeErrorCode.INVALID_NOTICE
        verify(exactly = 0) { savePort.save(any()) }
    }

    @Test
    fun `update는 대상이 없으면 NOTICE_NOT_FOUND`() {
        every { queryPort.findById(99) } returns null

        val e = shouldThrow<BusinessException> { service.update(99, UpdateNoticeCommand("새 내용", null, null)) }

        e.errorCode shouldBe NoticeErrorCode.NOTICE_NOT_FOUND
        verify(exactly = 0) { savePort.save(any()) }
    }

    @Test
    fun `update는 content만 바꾸고 게시일·플랫폼·유형을 유지해 저장한다`() {
        val original = existing()
        val saved = slot<Notice>()
        every { queryPort.findById(7) } returns original
        every { savePort.save(capture(saved)) } answers { saved.captured }

        service.update(7, UpdateNoticeCommand("새 내용", null, null))

        saved.captured shouldBe original.copy(content = "새 내용")
    }

    @Test
    fun `update는 모든 값이 null이면 INVALID_NOTICE`() {
        every { queryPort.findById(7) } returns existing()

        val e = shouldThrow<BusinessException> { service.update(7, UpdateNoticeCommand(null, null, null)) }

        e.errorCode shouldBe NoticeErrorCode.INVALID_NOTICE
        verify(exactly = 0) { savePort.save(any()) }
    }
}
