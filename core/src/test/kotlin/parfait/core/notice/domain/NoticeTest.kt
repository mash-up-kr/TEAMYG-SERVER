package parfait.core.notice.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.notice.exception.NoticeErrorCode
import java.time.LocalDate
import java.time.LocalDateTime

class NoticeTest {
    private val publishedDate = LocalDate.of(2026, 11, 9)
    private val createdAt = LocalDateTime.of(2026, 11, 9, 10, 0)

    private fun create(
        type: NoticeType,
        platform: NoticePlatform = NoticePlatform.IOS,
        version: String? = null,
        revisionTarget: NoticeRevisionTarget? = null,
        content: String = "내용",
    ): Notice =
        Notice.create(
            type = type,
            platform = platform,
            version = version,
            revisionTarget = revisionTarget,
            content = content,
            publishedDate = publishedDate,
            createdAt = createdAt,
        )

    private fun shouldBeInvalid(block: () -> Unit) {
        shouldThrow<BusinessException>(block).errorCode shouldBe NoticeErrorCode.INVALID_NOTICE
    }

    private fun update(version: String = "1.3.0") = create(NoticeType.UPDATE, version = version)

    private fun guide(target: NoticeRevisionTarget = NoticeRevisionTarget.BOTH) =
        create(NoticeType.GUIDE, revisionTarget = target)

    @Test
    fun `create 업데이트 공지는 버전으로 제목을 만든다`() {
        val notice = update()

        notice.title shouldBe "파르페 v1.3.0 업데이트 안내"
        notice.version shouldBe "1.3.0"
        notice.id shouldBe null
    }

    @Test
    fun `create 업데이트 공지는 COMMON 플랫폼도 허용한다`() {
        val notice = create(NoticeType.UPDATE, platform = NoticePlatform.COMMON, version = "2.0.1")

        notice.platform shouldBe NoticePlatform.COMMON
        notice.title shouldBe "파르페 v2.0.1 업데이트 안내"
    }

    @Test
    fun `create 안내 공지는 개정 대상별 제목을 만든다`() {
        guide(NoticeRevisionTarget.BOTH).title shouldBe "이용약관 및 개인정보처리방침 개정 안내"
        guide(NoticeRevisionTarget.TERMS).title shouldBe "이용약관 개정 안내"
        guide(NoticeRevisionTarget.PRIVACY).title shouldBe "개인정보처리방침 개정 안내"
    }

    @Test
    fun `create 업데이트 공지에 버전이 없거나 형식이 틀리면 INVALID_NOTICE`() {
        listOf<String?>(null, "1.3", "v1.3.0", "1.3.0.1").forEach { version ->
            shouldBeInvalid { create(NoticeType.UPDATE, version = version) }
        }
    }

    @Test
    fun `create 업데이트 공지의 버전 각 자리가 5자리를 넘거나 20자를 넘으면 INVALID_NOTICE`() {
        val versions = listOf("123456.1.1", "1.123456.1", "1.1.123456", "12345.12345.12345.1", "1".repeat(21) + ".1.1")
        versions.forEach { version ->
            shouldBeInvalid { create(NoticeType.UPDATE, version = version) }
        }
    }

    @Test
    fun `create 업데이트 공지는 경계 길이의 버전을 허용한다`() {
        update("1.3.0").version shouldBe "1.3.0"
        update("12345.12345.12345").version shouldBe "12345.12345.12345"
    }

    @Test
    fun `create 업데이트 공지에 revisionTarget이 있으면 INVALID_NOTICE`() {
        shouldBeInvalid {
            create(NoticeType.UPDATE, version = "1.3.0", revisionTarget = NoticeRevisionTarget.TERMS)
        }
    }

    @Test
    fun `create 안내 공지에 revisionTarget이 없거나 version이 있으면 INVALID_NOTICE`() {
        shouldBeInvalid { create(NoticeType.GUIDE) }
        shouldBeInvalid {
            create(NoticeType.GUIDE, version = "1.3.0", revisionTarget = NoticeRevisionTarget.TERMS)
        }
    }

    @Test
    fun `create content가 공백뿐이면 INVALID_NOTICE`() {
        shouldBeInvalid { create(NoticeType.UPDATE, version = "1.3.0", content = "   ") }
    }

    @Test
    fun `revise content만 바꾸면 제목과 게시일이 그대로다`() {
        val revised = update().revise(content = "새 내용", version = null, revisionTarget = null)

        revised.content shouldBe "새 내용"
        revised.title shouldBe "파르페 v1.3.0 업데이트 안내"
        revised.publishedDate shouldBe publishedDate
        revised.createdAt shouldBe createdAt
        revised.type shouldBe NoticeType.UPDATE
        revised.platform shouldBe NoticePlatform.IOS
    }

    @Test
    fun `revise 업데이트 공지의 version을 바꾸면 제목이 다시 만들어진다`() {
        val revised = update().revise(content = null, version = "1.4.0", revisionTarget = null)

        revised.version shouldBe "1.4.0"
        revised.title shouldBe "파르페 v1.4.0 업데이트 안내"
        revised.content shouldBe "내용"
    }

    @Test
    fun `revise 안내 공지의 revisionTarget을 바꾸면 제목이 다시 만들어진다`() {
        val revised =
            guide(
                NoticeRevisionTarget.BOTH,
            ).revise(content = null, version = null, revisionTarget = NoticeRevisionTarget.PRIVACY)

        revised.revisionTarget shouldBe NoticeRevisionTarget.PRIVACY
        revised.title shouldBe "개인정보처리방침 개정 안내"
    }

    @Test
    fun `revise 안내 공지에 version을 주면 INVALID_NOTICE`() {
        shouldBeInvalid { guide().revise(content = null, version = "1.3.0", revisionTarget = null) }
    }

    @Test
    fun `revise 모든 값이 null이면 INVALID_NOTICE`() {
        shouldBeInvalid { update().revise(content = null, version = null, revisionTarget = null) }
    }
}
