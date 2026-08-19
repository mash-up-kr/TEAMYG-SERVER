package parfait.core.parfaitimage.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderCommand
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class UpdateParfaitImageBorderServiceTest {
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val parfaitImageQueryPort = mockk<ParfaitImageQueryPort>()
    private val parfaitImageSavePort = mockk<ParfaitImageSavePort>()
    private val service =
        UpdateParfaitImageBorderService(
            parfaitGroupMemberQueryPort = parfaitGroupMemberQueryPort,
            parfaitQueryPort = parfaitQueryPort,
            parfaitImageQueryPort = parfaitImageQueryPort,
            parfaitImageSavePort = parfaitImageSavePort,
        )

    private val owner =
        ParfaitGroupMember.reconstitute(
            id = 10L,
            parfaitGroupId = 1L,
            memberId = 42L,
            groupNickname = "연경이",
            joinedAt = LocalDateTime.now(),
        )

    private fun existingImage(): ParfaitImage =
        ParfaitImage.reconstitute(
            id = 201L,
            parfaitId = 5L,
            imageMetaId = 77L,
            placedByGroupMemberId = 10L,
            imageUrl = "https://s3.example/nukki/user42/uuid.png",
            positionX = 120.5,
            positionY = 340.2,
            positionZ = 1,
            scale = 1.0,
            rotation = 0.0,
            borderType = BorderType.NONE,
            borderColor = null,
            borderWidth = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun activeParfait(status: ParfaitStatus = ParfaitStatus.ACTIVE): Parfait =
        Parfait.reconstitute(
            id = 5L,
            parfaitGroupId = 1L,
            parfaitDate = LocalDate.of(2026, 7, 9),
            status = status,
            backgroundType = null,
            backgroundValue = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun command(
        borderType: BorderType = BorderType.SOLID,
        borderColor: String? = "#FF0000",
        borderWidth: Double? = 3.0,
    ) = UpdateParfaitImageBorderCommand(
        memberId = 42L,
        groupId = 1L,
        parfaitId = 5L,
        parfaitImageId = 201L,
        borderType = borderType,
        borderColor = borderColor,
        borderWidth = borderWidth,
    )

    @Test
    fun `배치한 본인이 요청하면 테두리를 갱신하고 위치는 그대로 유지한다`() {
        every { parfaitImageQueryPort.findById(201L) } returns existingImage()
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageSavePort.save(any()) } answers { firstArg() }

        val result = service.update(command())

        result.parfaitImageId shouldBe 201L
        result.borderType shouldBe BorderType.SOLID
        result.borderColor shouldBe "#FF0000"
        result.borderWidth shouldBe 3.0
    }

    @Test
    fun `그룹 소속의 파르페가 아니면 PARFAIT_NOT_FOUND 예외를 던진다`() {
        every { parfaitImageQueryPort.findById(201L) } returns existingImage()
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns null

        val exception = assertFailsWith<BusinessException> { service.update(command()) }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_NOT_FOUND
    }

    @Test
    fun `이미 마감된 파르페면 PARFAIT_ALREADY_CLOSED 예외를 던진다`() {
        every { parfaitImageQueryPort.findById(201L) } returns existingImage()
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait(status = ParfaitStatus.EMPTY)

        val exception = assertFailsWith<BusinessException> { service.update(command()) }
        exception.errorCode shouldBe ParfaitErrorCode.PARFAIT_ALREADY_CLOSED
    }

    @Test
    fun `존재하지 않는 배치 ID면 PARFAIT_IMAGE_NOT_FOUND 예외를 던진다`() {
        every { parfaitImageQueryPort.findById(999L) } returns null

        val exception =
            assertFailsWith<BusinessException> {
                service.update(command().copy(parfaitImageId = 999L))
            }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_FOUND
    }

    @Test
    fun `배치한 본인이 아니면 PARFAIT_IMAGE_NOT_OWNED 예외를 던진다`() {
        every { parfaitImageQueryPort.findById(201L) } returns existingImage()
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns
            ParfaitGroupMember.reconstitute(
                id = 999L,
                parfaitGroupId = 1L,
                memberId = 42L,
                groupNickname = "다른사람",
                joinedAt = LocalDateTime.now(),
            )

        val exception = assertFailsWith<BusinessException> { service.update(command()) }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED
    }

    @Test
    fun `SOLID인데 색상이나 두께가 없으면 INVALID_BORDER 예외를 던진다`() {
        every { parfaitImageQueryPort.findById(201L) } returns existingImage()
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()

        val exception =
            assertFailsWith<BusinessException> {
                service.update(command(borderColor = null, borderWidth = null))
            }
        exception.errorCode shouldBe ParfaitImageErrorCode.INVALID_BORDER
    }
}
