package parfait.core.parfaitimage.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderCommand
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class UpdateParfaitImageBorderServiceTest {
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitImageQueryPort = mockk<ParfaitImageQueryPort>()
    private val parfaitImageSavePort = mockk<ParfaitImageSavePort>()
    private val service =
        UpdateParfaitImageBorderService(
            parfaitGroupMemberQueryPort = parfaitGroupMemberQueryPort,
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
        every { parfaitImageSavePort.save(any()) } answers { firstArg() }

        val result = service.update(command())

        result.parfaitImageId shouldBe 201L
        result.borderType shouldBe BorderType.SOLID
        result.borderColor shouldBe "#FF0000"
        result.borderWidth shouldBe 3.0
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

        val exception =
            assertFailsWith<BusinessException> {
                service.update(command(borderColor = null, borderWidth = null))
            }
        exception.errorCode shouldBe ParfaitImageErrorCode.INVALID_BORDER
    }
}
