package parfait.core.parfait.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.port.`in`.EnsureActiveCanvasUseCase
import parfait.core.parfait.port.`in`.GetTodayParfaitCommand
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class GetTodayParfaitServiceTest {
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val ensureActiveCanvasUseCase = mockk<EnsureActiveCanvasUseCase>()
    private val parfaitImageQueryPort = mockk<ParfaitImageQueryPort>()
    private val service =
        GetTodayParfaitService(
            parfaitGroupMemberQueryPort,
            parfaitQueryPort,
            ensureActiveCanvasUseCase,
            parfaitImageQueryPort,
        )

    private fun groupMember(
        id: Long,
        nickname: String,
    ): ParfaitGroupMember =
        ParfaitGroupMember.reconstitute(
            id = id,
            parfaitGroupId = 1L,
            memberId = id * 100,
            groupNickname = nickname,
            joinedAt = LocalDateTime.now(),
        )

    @Test
    fun `오늘자 파르페가 없으면 자동 생성한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { ensureActiveCanvasUseCase.ensure(1L, any()) } returns
            Parfait.reconstitute(
                id = 100L,
                parfaitGroupId = 1L,
                parfaitDate = LocalDate.now(),
                status = ParfaitStatus.ACTIVE,
                backgroundType = null,
                backgroundValue = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        every { parfaitQueryPort.findLastClosedDateByGroupId(1L) } returns null
        every { parfaitGroupMemberQueryPort.findAllByGroupId(1L) } returns emptyList()
        every { parfaitImageQueryPort.findAllByParfaitId(100L) } returns emptyList()

        val result = service.get(GetTodayParfaitCommand(memberId = 10L, groupId = 1L))

        result.parfaitId shouldBe 100L
        result.status shouldBe ParfaitStatus.ACTIVE
        verify(exactly = 1) { ensureActiveCanvasUseCase.ensure(1L, any()) }
    }

    @Test
    fun `오늘자 파르페가 이미 있으면 재사용한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { ensureActiveCanvasUseCase.ensure(1L, any()) } returns
            Parfait.reconstitute(
                id = 200L,
                parfaitGroupId = 1L,
                parfaitDate = LocalDate.now(),
                status = ParfaitStatus.ACTIVE,
                backgroundType = null,
                backgroundValue = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        every { parfaitQueryPort.findLastClosedDateByGroupId(1L) } returns LocalDate.of(2026, 7, 8)
        every { parfaitGroupMemberQueryPort.findAllByGroupId(1L) } returns emptyList()
        every { parfaitImageQueryPort.findAllByParfaitId(200L) } returns emptyList()

        val result = service.get(GetTodayParfaitCommand(memberId = 10L, groupId = 1L))

        result.parfaitId shouldBe 200L
        result.lastClosedDate shouldBe LocalDate.of(2026, 7, 8)
        verify(exactly = 1) { ensureActiveCanvasUseCase.ensure(1L, any()) }
    }

    @Test
    fun `배치된 토핑이 없으면 images는 null이다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { ensureActiveCanvasUseCase.ensure(1L, any()) } returns
            Parfait.reconstitute(
                id = 200L,
                parfaitGroupId = 1L,
                parfaitDate = LocalDate.now(),
                status = ParfaitStatus.ACTIVE,
                backgroundType = null,
                backgroundValue = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        every { parfaitQueryPort.findLastClosedDateByGroupId(1L) } returns null
        every { parfaitGroupMemberQueryPort.findAllByGroupId(1L) } returns emptyList()
        every { parfaitImageQueryPort.findAllByParfaitId(200L) } returns emptyList()

        val result = service.get(GetTodayParfaitCommand(memberId = 10L, groupId = 1L))

        result.images shouldBe null
        result.background shouldBe null
    }

    @Test
    fun `배치된 토핑이 있으면 배치자 닉네임을 붙여 반환한다 (탈퇴 멤버 포함)`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { ensureActiveCanvasUseCase.ensure(1L, any()) } returns
            Parfait.reconstitute(
                id = 200L,
                parfaitGroupId = 1L,
                parfaitDate = LocalDate.now(),
                status = ParfaitStatus.ACTIVE,
                backgroundType = null,
                backgroundValue = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        every { parfaitQueryPort.findLastClosedDateByGroupId(1L) } returns null
        every { parfaitGroupMemberQueryPort.findAllByGroupId(1L) } returns
            listOf(groupMember(10L, "연경이"))

        val placedImage =
            ParfaitImage.reconstitute(
                id = 201L,
                parfaitId = 200L,
                imageMetaId = 77L,
                placedByGroupMemberId = 11L,
                imageUrl = "https://parfait-bucket.s3.../nukki/user1/550e8400.png",
                positionX = 120.5,
                positionY = 340.2,
                positionZ = 1,
                scale = 1.0,
                rotation = 0.0,
                borderType = BorderType.SOLID,
                borderColor = "#000000",
                borderWidth = 2.0,
                createdAt = LocalDateTime.of(2026, 7, 9, 14, 30, 0),
                updatedAt = LocalDateTime.of(2026, 7, 9, 14, 30, 0),
            )
        every { parfaitImageQueryPort.findAllByParfaitId(200L) } returns listOf(placedImage)
        every { parfaitGroupMemberQueryPort.findAllByIds(listOf(11L)) } returns
            listOf(groupMember(11L, "(알수없음)"))

        val result = service.get(GetTodayParfaitCommand(memberId = 10L, groupId = 1L))

        result.images?.size shouldBe 1
        result.images
            ?.first()
            ?.placedBy
            ?.groupMemberId shouldBe 11L
        result.images
            ?.first()
            ?.placedBy
            ?.nickname shouldBe "(알수없음)"
    }

    @Test
    fun `배경이 모두 설정되어 있으면 background를 채워 반환한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { ensureActiveCanvasUseCase.ensure(1L, any()) } returns
            Parfait.reconstitute(
                id = 200L,
                parfaitGroupId = 1L,
                parfaitDate = LocalDate.now(),
                status = ParfaitStatus.ACTIVE,
                backgroundType = BackgroundType.COLOR,
                backgroundValue = "#FFFFFF",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        every { parfaitQueryPort.findLastClosedDateByGroupId(1L) } returns null
        every { parfaitGroupMemberQueryPort.findAllByGroupId(1L) } returns emptyList()
        every { parfaitImageQueryPort.findAllByParfaitId(200L) } returns emptyList()

        val result = service.get(GetTodayParfaitCommand(memberId = 10L, groupId = 1L))

        result.background?.type shouldBe BackgroundType.COLOR
        result.background?.value shouldBe "#FFFFFF"
    }

    @Test
    fun `그룹에 참여하지 않은 회원은 조회를 거부한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns false

        val exception =
            assertFailsWith<ParfaitGroupException> {
                service.get(GetTodayParfaitCommand(memberId = 10L, groupId = 1L))
            }
        exception.error shouldBe ParfaitGroupError.GROUP_NOT_JOINED
    }
}
