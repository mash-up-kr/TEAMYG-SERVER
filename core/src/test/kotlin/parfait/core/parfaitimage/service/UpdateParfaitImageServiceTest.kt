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
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageCommand
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class UpdateParfaitImageServiceTest {
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val parfaitImageQueryPort = mockk<ParfaitImageQueryPort>()
    private val parfaitImageSavePort = mockk<ParfaitImageSavePort>()
    private val service =
        UpdateParfaitImageService(
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
        positionX: Double? = 200.0,
        positionY: Double? = 400.0,
        positionZ: Int? = null,
        scale: Double? = 1.5,
        rotation: Double? = 45.0,
    ) = UpdateParfaitImageCommand(
        memberId = 42L,
        groupId = 1L,
        parfaitId = 5L,
        parfaitImageId = 201L,
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    )

    @Test
    fun `배치한 본인이 요청하면 지정한 필드만 갱신하고 나머지는 유지한다`() {
        every { parfaitImageQueryPort.findById(201L) } returns existingImage()
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageSavePort.save(any()) } answers { firstArg() }

        val result = service.update(command())

        result.parfaitImageId shouldBe 201L
        result.positionX shouldBe 200.0
        result.positionY shouldBe 400.0
        result.positionZ shouldBe 1
        result.scale shouldBe 1.5
        result.rotation shouldBe 45.0
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
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait(status = ParfaitStatus.CLOSED)

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
    fun `경로의 parfaitId와 배치의 parfaitId가 다르면 PARFAIT_IMAGE_NOT_FOUND 예외를 던진다`() {
        every { parfaitImageQueryPort.findById(201L) } returns existingImage()

        val exception =
            assertFailsWith<BusinessException> {
                service.update(command().copy(parfaitId = 999L))
            }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_FOUND
    }

    @Test
    fun `배치한 본인이 아니면 PARFAIT_IMAGE_NOT_OWNED 예외를 던진다`() {
        every { parfaitImageQueryPort.findById(201L) } returns existingImage()
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns
            owner.let {
                ParfaitGroupMember.reconstitute(
                    id = 999L,
                    parfaitGroupId = 1L,
                    memberId = 42L,
                    groupNickname = "다른사람",
                    joinedAt = LocalDateTime.now(),
                )
            }

        val exception = assertFailsWith<BusinessException> { service.update(command()) }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED
    }

    @Test
    fun `그룹 멤버가 아니면 PARFAIT_IMAGE_NOT_OWNED 예외를 던진다`() {
        every { parfaitImageQueryPort.findById(201L) } returns existingImage()
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns null

        val exception = assertFailsWith<BusinessException> { service.update(command()) }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED
    }
}
