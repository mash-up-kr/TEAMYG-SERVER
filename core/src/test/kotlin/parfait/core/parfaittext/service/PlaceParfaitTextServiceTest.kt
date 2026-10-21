package parfait.core.parfaittext.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.OwnerType
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.service.ParfaitVersionService
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaittext.domain.ParfaitText
import parfait.core.parfaittext.exception.ParfaitTextErrorCode
import parfait.core.parfaittext.port.`in`.PlaceParfaitTextCommand
import parfait.core.parfaittext.port.out.ParfaitTextSavePort
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class PlaceParfaitTextServiceTest {
    private val groupMembers = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaits = mockk<ParfaitQueryPort>()
    private val texts = mockk<ParfaitTextSavePort>()
    private val versions = mockk<ParfaitVersionService>(relaxed = true)
    private val service = PlaceParfaitTextService(groupMembers, parfaits, texts, versions)
    private val command =
        PlaceParfaitTextCommand(
            memberId = 42L,
            groupId = 1L,
            parfaitId = 5L,
            text = "안녕\n파르페! 🎉",
            positionX = 120.5,
            positionY = 340.2,
            positionZ = 2,
            scale = 1.5,
            rotation = -30.0,
            color = "#FF5733",
            fontSize = 16.0,
        )

    private fun parfait(status: ParfaitStatus = ParfaitStatus.ACTIVE): Parfait =
        Parfait.reconstitute(
            id = 5L,
            parfaitGroupId = 1L,
            parfaitDate = LocalDate.of(2026, 10, 20),
            status = status,
            backgroundType = null,
            backgroundValue = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @BeforeEach
    fun setUp() {
        every { groupMembers.findByGroupIdAndMemberId(1L, 42L) } returns
            ParfaitGroupMember.reconstitute(
                id = 10L,
                parfaitGroupId = 1L,
                memberId = 42L,
                groupNickname = "연경이",
                joinedAt = LocalDateTime.now(),
            )
        every { parfaits.findByIdAndGroupId(5L, 1L) } returns parfait()
        every { texts.save(any()) } answers {
            val text = firstArg<ParfaitText>()
            ParfaitText.reconstitute(
                id = 301L,
                parfaitId = text.parfaitId,
                placedByGroupMemberId = text.placedByGroupMemberId,
                text = text.text,
                positionX = text.positionX,
                positionY = text.positionY,
                positionZ = text.positionZ,
                scale = text.scale,
                rotation = text.rotation,
                color = text.color,
                fontSize = text.fontSize,
                createdAt = text.createdAt,
                updatedAt = text.updatedAt,
            )
        }
    }

    @Test
    fun `텍스트와 스타일을 저장하고 저장 전에 캔버스 버전을 올린다`() {
        val result = service.place(command)

        result.parfaitTextId shouldBe 301L
        result.text shouldBe command.text
        result.positionX shouldBe command.positionX
        result.positionY shouldBe command.positionY
        result.positionZ shouldBe command.positionZ
        result.scale shouldBe command.scale
        result.rotation shouldBe command.rotation
        result.color shouldBe command.color
        result.fontSize shouldBe command.fontSize
        result.placedBy.groupMemberId shouldBe 10L
        result.placedBy.nickname shouldBe "연경이"
        result.placedBy.ownerType shouldBe OwnerType.ME
        verifyOrder {
            versions.bump(5L)
            texts.save(match { it.id == null && it.parfaitId == 5L && it.placedByGroupMemberId == 10L })
        }
    }

    @Test
    fun `그룹 미참여자는 텍스트를 배치할 수 없다`() {
        every { groupMembers.findByGroupIdAndMemberId(1L, 42L) } returns null

        val exception = assertFailsWith<ParfaitGroupException> { service.place(command) }
        exception.error shouldBe ParfaitGroupError.GROUP_NOT_JOINED
        verify(exactly = 0) { parfaits.findByIdAndGroupId(any(), any()) }
        verifyNoWrites()
    }

    @Test
    fun `존재하지 않거나 다른 그룹의 파르페에는 배치할 수 없다`() {
        every { parfaits.findByIdAndGroupId(5L, 1L) } returns null

        val exception = assertFailsWith<BusinessException> { service.place(command) }
        exception.errorCode shouldBe ParfaitErrorCode.PARFAIT_NOT_FOUND
        verifyNoWrites()
    }

    @Test
    fun `마감되거나 빈 상태로 종료된 파르페에는 배치할 수 없다`() {
        listOf(ParfaitStatus.CLOSED, ParfaitStatus.EMPTY).forEach { status ->
            every { parfaits.findByIdAndGroupId(5L, 1L) } returns parfait(status)

            val exception = assertFailsWith<BusinessException> { service.place(command) }
            exception.errorCode shouldBe ParfaitErrorCode.PARFAIT_ALREADY_CLOSED
        }
        verifyNoWrites()
    }

    @Test
    fun `공백 텍스트나 2000자를 초과한 텍스트는 저장하지 않는다`() {
        listOf("", " \n\t", "가".repeat(2001)).forEach { text ->
            assertFailsWith<BusinessException> { service.place(command.copy(text = text)) }.errorCode shouldBe
                ParfaitTextErrorCode.INVALID_TEXT
        }
        verifyNoWrites()
    }

    @Test
    fun `유효하지 않은 스타일은 버전을 올리거나 저장하지 않는다`() {
        listOf(
            command.copy(color = " "),
            command.copy(color = "a".repeat(21)),
            command.copy(scale = 0.0),
            command.copy(scale = -1.0),
            command.copy(scale = Double.POSITIVE_INFINITY),
            command.copy(fontSize = 0.0),
            command.copy(fontSize = -1.0),
            command.copy(fontSize = Double.NaN),
            command.copy(positionX = Double.NaN),
            command.copy(positionY = Double.NEGATIVE_INFINITY),
            command.copy(rotation = Double.POSITIVE_INFINITY),
        ).forEach { invalid ->
            assertFailsWith<BusinessException> { service.place(invalid) }.errorCode shouldBe
                ParfaitTextErrorCode.INVALID_TEXT_STYLE
        }
        verifyNoWrites()
    }

    @Test
    fun `2000자 텍스트와 양수 소수 폰트 크기를 허용한다`() {
        val result = service.place(command.copy(text = "가".repeat(2000), fontSize = 0.5, scale = 0.25))

        result.text.length shouldBe 2000
        result.fontSize shouldBe 0.5
        result.scale shouldBe 0.25
    }

    private fun verifyNoWrites() {
        verify(exactly = 0) { versions.bump(any()) }
        verify(exactly = 0) { texts.save(any()) }
    }
}
