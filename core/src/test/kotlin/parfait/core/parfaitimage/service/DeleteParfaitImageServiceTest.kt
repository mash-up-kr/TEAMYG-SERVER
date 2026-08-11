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
import parfait.core.image.port.out.ImageDeletePort
import parfait.core.image.port.out.ImageMetaQueryPort
import parfait.core.image.port.out.ImageMetaSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.DeleteParfaitImageCommand
import parfait.core.parfaitimage.port.out.ParfaitImageDeletePort
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class DeleteParfaitImageServiceTest {
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitImageQueryPort = mockk<ParfaitImageQueryPort>()
    private val parfaitImageDeletePort = mockk<ParfaitImageDeletePort>(relaxed = true)
    private val imageMetaQueryPort = mockk<ImageMetaQueryPort>()
    private val imageMetaSavePort = mockk<ImageMetaSavePort>()
    private val imageDeletePort = mockk<ImageDeletePort>(relaxed = true)
    private val service =
        DeleteParfaitImageService(
            parfaitGroupMemberQueryPort = parfaitGroupMemberQueryPort,
            parfaitImageQueryPort = parfaitImageQueryPort,
            parfaitImageDeletePort = parfaitImageDeletePort,
            imageMetaQueryPort = imageMetaQueryPort,
            imageMetaSavePort = imageMetaSavePort,
            imageDeletePort = imageDeletePort,
        )

    private val owner =
        ParfaitGroupMember.reconstitute(
            id = 10L,
            parfaitGroupId = 1L,
            memberId = 42L,
            groupNickname = "연경이",
            joinedAt = LocalDateTime.now(),
        )

    private fun placedImage(): ParfaitImage =
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

    private fun imageMeta(referenceCount: Long): ImageMeta =
        ImageMeta.reconstitute(
            id = 77L,
            url = "https://s3.example/nukki/user42/uuid.png",
            uploadedByMemberId = 42L,
            imageType = ImageType.NUKKI,
            status = ImageStatus.COMPLETED,
            referenceCount = referenceCount,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun command() =
        DeleteParfaitImageCommand(
            memberId = 42L,
            groupId = 1L,
            parfaitId = 5L,
            parfaitImageId = 201L,
        )

    @Test
    fun `참조 수가 0이 되면 배치를 삭제하고 S3 이미지도 삭제한다`() {
        every { parfaitImageQueryPort.findById(201L) } returns placedImage()
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { imageMetaQueryPort.findById(77L) } returns imageMeta(referenceCount = 1)
        every { imageMetaSavePort.save(any()) } answers { firstArg() }

        service.delete(command())

        verify { parfaitImageDeletePort.deleteById(201L) }
        verify { imageMetaSavePort.save(match { it.referenceCount == 0L }) }
        verify { imageDeletePort.delete("https://s3.example/nukki/user42/uuid.png") }
    }

    @Test
    fun `참조 수가 남아 있으면 S3 이미지는 삭제하지 않는다`() {
        every { parfaitImageQueryPort.findById(201L) } returns placedImage()
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { imageMetaQueryPort.findById(77L) } returns imageMeta(referenceCount = 2)
        every { imageMetaSavePort.save(any()) } answers { firstArg() }

        service.delete(command())

        verify { imageMetaSavePort.save(match { it.referenceCount == 1L }) }
        verify(exactly = 0) { imageDeletePort.delete(any()) }
    }

    @Test
    fun `존재하지 않는 배치 ID면 PARFAIT_IMAGE_NOT_FOUND 예외를 던진다`() {
        every { parfaitImageQueryPort.findById(999L) } returns null

        val exception =
            assertFailsWith<BusinessException> {
                service.delete(command().copy(parfaitImageId = 999L))
            }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_FOUND
    }

    @Test
    fun `배치한 본인이 아니면 PARFAIT_IMAGE_NOT_OWNED 예외를 던진다`() {
        every { parfaitImageQueryPort.findById(201L) } returns placedImage()
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns
            ParfaitGroupMember.reconstitute(
                id = 999L,
                parfaitGroupId = 1L,
                memberId = 42L,
                groupNickname = "다른사람",
                joinedAt = LocalDateTime.now(),
            )

        val exception = assertFailsWith<BusinessException> { service.delete(command()) }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED
    }
}
