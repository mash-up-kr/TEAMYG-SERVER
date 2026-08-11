package parfait.core.parfaitimage.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.image.domain.ImageMeta
import parfait.core.image.domain.ImageStatus
import parfait.core.image.domain.ImageType
import parfait.core.image.exception.ImageErrorCode
import parfait.core.image.port.out.ImageMetaQueryPort
import parfait.core.image.port.out.ImageMetaSavePort
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageCommand
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class PlaceParfaitImageServiceTest {
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val imageMetaQueryPort = mockk<ImageMetaQueryPort>()
    private val imageMetaSavePort = mockk<ImageMetaSavePort>()
    private val parfaitImageQueryPort = mockk<ParfaitImageQueryPort>()
    private val parfaitImageSavePort = mockk<ParfaitImageSavePort>()
    private val service =
        PlaceParfaitImageService(
            parfaitGroupMemberQueryPort = parfaitGroupMemberQueryPort,
            parfaitQueryPort = parfaitQueryPort,
            imageMetaQueryPort = imageMetaQueryPort,
            imageMetaSavePort = imageMetaSavePort,
            parfaitImageQueryPort = parfaitImageQueryPort,
            parfaitImageSavePort = parfaitImageSavePort,
        )

    private val groupMember =
        ParfaitGroupMember.reconstitute(
            id = 10L,
            parfaitGroupId = 1L,
            memberId = 42L,
            groupNickname = "연경이",
            joinedAt = LocalDateTime.now(),
        )

    private fun confirmedImage(): ImageMeta =
        ImageMeta.reconstitute(
            id = 77L,
            url = "https://s3.example/nukki/user42/uuid.png",
            uploadedByMemberId = 42L,
            imageType = ImageType.NUKKI,
            status = ImageStatus.COMPLETED,
            referenceCount = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun command(
        borderType: BorderType = BorderType.NONE,
        borderColor: String? = null,
        borderWidth: Double? = null,
    ) = PlaceParfaitImageCommand(
        memberId = 42L,
        groupId = 1L,
        parfaitId = 5L,
        imageId = 77L,
        positionX = 120.5,
        positionY = 340.2,
        positionZ = 1,
        scale = 1.0,
        rotation = 0.0,
        borderType = borderType,
        borderColor = borderColor,
        borderWidth = borderWidth,
    )

    private fun stubHappyPathPrerequisites() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns groupMember
        every { parfaitQueryPort.existsByIdAndGroupId(5L, 1L) } returns true
        every { imageMetaQueryPort.findById(77L) } returns confirmedImage()
        every { imageMetaSavePort.save(any()) } answers { firstArg() }
        every { parfaitImageSavePort.save(any()) } answers {
            val saved = firstArg<ParfaitImage>()
            if (saved.id != null) {
                saved
            } else {
                ParfaitImage.reconstitute(
                    id = 201L,
                    parfaitId = saved.parfaitId,
                    imageMetaId = saved.imageMetaId,
                    placedByGroupMemberId = saved.placedByGroupMemberId,
                    imageUrl = saved.imageUrl,
                    positionX = saved.positionX,
                    positionY = saved.positionY,
                    positionZ = saved.positionZ,
                    scale = saved.scale,
                    rotation = saved.rotation,
                    borderType = saved.borderType,
                    borderColor = saved.borderColor,
                    borderWidth = saved.borderWidth,
                    createdAt = saved.createdAt,
                    updatedAt = saved.updatedAt,
                )
            }
        }
    }

    @Test
    fun `처음 배치하면 신규로 저장하고 배치 결과를 반환한다`() {
        stubHappyPathPrerequisites()
        every { parfaitImageQueryPort.findByParfaitIdAndImageMetaId(5L, 77L) } returns null

        val result = service.place(command())

        result.imageId shouldBe 77L
        result.imageUrl shouldBe "https://s3.example/nukki/user42/uuid.png"
        result.positionX shouldBe 120.5
        result.placedBy.groupMemberId shouldBe 10L
        result.placedBy.nickname shouldBe "연경이"
        verify { imageMetaSavePort.save(match { it.referenceCount == 1L }) }
    }

    @Test
    fun `이미 배치된 이미지를 다시 확정하면 기존 배치를 갱신한다`() {
        stubHappyPathPrerequisites()
        val existing =
            ParfaitImage.reconstitute(
                id = 201L,
                parfaitId = 5L,
                imageMetaId = 77L,
                placedByGroupMemberId = 99L,
                imageUrl = "https://s3.example/nukki/user42/uuid.png",
                positionX = 0.0,
                positionY = 0.0,
                positionZ = 0,
                scale = 1.0,
                rotation = 0.0,
                borderType = BorderType.NONE,
                borderColor = null,
                borderWidth = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        every { parfaitImageQueryPort.findByParfaitIdAndImageMetaId(5L, 77L) } returns existing

        val result = service.place(command())

        result.parfaitImageId shouldBe 201L
        result.positionX shouldBe 120.5
        result.placedBy.groupMemberId shouldBe 10L
        verify(exactly = 0) { imageMetaSavePort.save(any()) }
    }

    @Test
    fun `그룹에 참여하지 않았으면 GROUP_NOT_JOINED 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns null

        val exception = assertFailsWith<ParfaitGroupException> { service.place(command()) }
        exception.error shouldBe ParfaitGroupError.GROUP_NOT_JOINED
    }

    @Test
    fun `그룹 소속의 파르페가 아니면 PARFAIT_NOT_FOUND 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns groupMember
        every { parfaitQueryPort.existsByIdAndGroupId(5L, 1L) } returns false

        val exception = assertFailsWith<BusinessException> { service.place(command()) }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_NOT_FOUND
    }

    @Test
    fun `존재하지 않는 이미지면 IMAGE_NOT_FOUND 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns groupMember
        every { parfaitQueryPort.existsByIdAndGroupId(5L, 1L) } returns true
        every { imageMetaQueryPort.findById(77L) } returns null

        val exception = assertFailsWith<BusinessException> { service.place(command()) }
        exception.errorCode shouldBe ImageErrorCode.IMAGE_NOT_FOUND
    }

    @Test
    fun `아직 확인되지 않은(PENDING) 이미지면 IMAGE_NOT_CONFIRMED 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns groupMember
        every { parfaitQueryPort.existsByIdAndGroupId(5L, 1L) } returns true
        every { imageMetaQueryPort.findById(77L) } returns
            ImageMeta.reconstitute(
                id = 77L,
                url = "https://s3.example/nukki/user42/uuid.png",
                uploadedByMemberId = 42L,
                imageType = ImageType.NUKKI,
                status = ImageStatus.PENDING,
                referenceCount = 0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val exception = assertFailsWith<BusinessException> { service.place(command()) }
        exception.errorCode shouldBe ParfaitImageErrorCode.IMAGE_NOT_CONFIRMED
    }

    @Test
    fun `SOLID 테두리인데 색상이나 두께가 없으면 INVALID_BORDER 예외를 던진다`() {
        stubHappyPathPrerequisites()
        every { parfaitImageQueryPort.findByParfaitIdAndImageMetaId(5L, 77L) } returns null

        val exception =
            assertFailsWith<BusinessException> {
                service.place(command(borderType = BorderType.SOLID, borderColor = null, borderWidth = null))
            }
        exception.errorCode shouldBe ParfaitImageErrorCode.INVALID_BORDER
    }
}
