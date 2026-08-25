package parfait.core.parfait.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.domain.OwnerType
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.GetParfaitDetailCommand
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

class GetParfaitDetailServiceTest {
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val parfaitImageQueryPort = mockk<ParfaitImageQueryPort>()
    private val service = GetParfaitDetailService(parfaitGroupMemberQueryPort, parfaitQueryPort, parfaitImageQueryPort)

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

    private fun parfait(
        status: ParfaitStatus = ParfaitStatus.ACTIVE,
        backgroundType: BackgroundType? = null,
        backgroundValue: String? = null,
    ): Parfait =
        Parfait.reconstitute(
            id = 98L,
            parfaitGroupId = 1L,
            parfaitDate = LocalDate.of(2026, 7, 7),
            status = status,
            backgroundType = backgroundType,
            backgroundValue = backgroundValue,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `그룹원, 배경, 이미지를 포함한 파르페 상세를 반환한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findByIdAndGroupId(98L, 1L) } returns
            parfait(status = ParfaitStatus.CLOSED, backgroundType = BackgroundType.COLOR, backgroundValue = "#FFFFFF")
        every { parfaitQueryPort.findLastClosedDateByGroupId(1L) } returns LocalDate.of(2026, 7, 7)
        every { parfaitGroupMemberQueryPort.findAllByGroupId(1L) } returns listOf(groupMember(10L, "연경이"))

        val placedImage =
            ParfaitImage.reconstitute(
                id = 201L,
                parfaitId = 98L,
                imageMetaId = 77L,
                placedByGroupMemberId = 10L,
                imageUrl = "https://parfait-bucket.s3.../nukki/user1/550e8400.png",
                positionX = 120.5,
                positionY = 340.2,
                positionZ = 1,
                scale = 1.0,
                rotation = 0.0,
                borderType = BorderType.SOLID,
                borderColor = "#000000",
                borderWidth = 2.0,
                createdAt = LocalDateTime.of(2026, 7, 7, 14, 30, 0),
                updatedAt = LocalDateTime.of(2026, 7, 7, 14, 30, 0),
            )
        every { parfaitImageQueryPort.findAllByParfaitId(98L) } returns listOf(placedImage)
        every { parfaitGroupMemberQueryPort.findAllByIds(listOf(10L)) } returns listOf(groupMember(10L, "연경이"))

        val result = service.getDetail(GetParfaitDetailCommand(memberId = 10L, groupId = 1L, parfaitId = 98L))

        result.parfaitId shouldBe 98L
        result.status shouldBe ParfaitStatus.CLOSED
        result.lastClosedDate shouldBe LocalDate.of(2026, 7, 7)
        result.groupMembers.single().nickname shouldBe "연경이"
        result.background?.type shouldBe BackgroundType.COLOR
        result.images
            ?.single()
            ?.placedBy
            ?.nickname shouldBe "연경이"
    }

    @Test
    fun `배치된 토핑이 없으면 images는 null이다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findByIdAndGroupId(98L, 1L) } returns parfait()
        every { parfaitQueryPort.findLastClosedDateByGroupId(1L) } returns null
        every { parfaitGroupMemberQueryPort.findAllByGroupId(1L) } returns emptyList()
        every { parfaitImageQueryPort.findAllByParfaitId(98L) } returns emptyList()

        val result = service.getDetail(GetParfaitDetailCommand(memberId = 10L, groupId = 1L, parfaitId = 98L))

        result.images shouldBe null
        result.background shouldBe null
    }

    @Test
    fun `내가 배치한 토핑과 다른 멤버가 배치한 토핑의 ownerType을 구분한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 1000L) } returns true
        every { parfaitQueryPort.findByIdAndGroupId(98L, 1L) } returns parfait()
        every { parfaitQueryPort.findLastClosedDateByGroupId(1L) } returns null
        every { parfaitGroupMemberQueryPort.findAllByGroupId(1L) } returns
            listOf(groupMember(10L, "연경이"), groupMember(11L, "서휘"))

        val myImage =
            ParfaitImage.reconstitute(
                id = 201L,
                parfaitId = 98L,
                imageMetaId = 77L,
                placedByGroupMemberId = 10L,
                imageUrl = "https://parfait-bucket.s3.../nukki/user1/550e8400.png",
                positionX = 120.5,
                positionY = 340.2,
                positionZ = 1,
                scale = 1.0,
                rotation = 0.0,
                borderType = BorderType.SOLID,
                borderColor = "#000000",
                borderWidth = 2.0,
                createdAt = LocalDateTime.of(2026, 7, 7, 14, 30, 0),
                updatedAt = LocalDateTime.of(2026, 7, 7, 14, 30, 0),
            )
        val otherImage =
            ParfaitImage.reconstitute(
                id = 202L,
                parfaitId = 98L,
                imageMetaId = 78L,
                placedByGroupMemberId = 11L,
                imageUrl = "https://parfait-bucket.s3.../nukki/user2/660e8400.png",
                positionX = 10.0,
                positionY = 20.0,
                positionZ = 2,
                scale = 1.0,
                rotation = 0.0,
                borderType = BorderType.NONE,
                borderColor = null,
                borderWidth = null,
                createdAt = LocalDateTime.of(2026, 7, 7, 15, 0, 0),
                updatedAt = LocalDateTime.of(2026, 7, 7, 15, 0, 0),
            )
        every { parfaitImageQueryPort.findAllByParfaitId(98L) } returns listOf(myImage, otherImage)
        every { parfaitGroupMemberQueryPort.findAllByIds(listOf(10L, 11L)) } returns
            listOf(groupMember(10L, "연경이"), groupMember(11L, "서휘"))

        // groupMember(10L, ...)의 memberId는 10L * 100 = 1000L이므로, 요청자 memberId도 1000L로
        // 맞춰 "myImage는 본인이 배치함"인 상황을 만든다.
        val result = service.getDetail(GetParfaitDetailCommand(memberId = 1000L, groupId = 1L, parfaitId = 98L))

        result.images
            ?.get(0)
            ?.placedBy
            ?.ownerType shouldBe OwnerType.ME
        result.images
            ?.get(1)
            ?.placedBy
            ?.ownerType shouldBe OwnerType.OTHER
    }

    @Test
    fun `존재하지 않거나 다른 그룹 소속인 파르페면 PARFAIT_NOT_FOUND를 던진다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findByIdAndGroupId(98L, 1L) } returns null

        val exception =
            assertFailsWith<BusinessException> {
                service.getDetail(GetParfaitDetailCommand(memberId = 10L, groupId = 1L, parfaitId = 98L))
            }
        exception.errorCode shouldBe ParfaitErrorCode.PARFAIT_NOT_FOUND
    }

    @Test
    fun `그룹에 참여하지 않은 회원은 조회를 거부한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns false

        val exception =
            assertFailsWith<ParfaitGroupException> {
                service.getDetail(GetParfaitDetailCommand(memberId = 10L, groupId = 1L, parfaitId = 98L))
            }
        exception.error shouldBe ParfaitGroupError.GROUP_NOT_JOINED
    }
}
