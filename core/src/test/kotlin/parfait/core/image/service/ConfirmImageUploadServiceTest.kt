package parfait.core.image.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.image.domain.ImageMeta
import parfait.core.image.domain.ImageStatus
import parfait.core.image.domain.ImageType
import parfait.core.image.exception.ImageErrorCode
import parfait.core.image.port.`in`.ConfirmImageUploadCommand
import parfait.core.image.port.out.ImageMetaQueryPort
import parfait.core.image.port.out.ImageMetaSavePort
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class ConfirmImageUploadServiceTest {
    private val imageMetaQueryPort = mockk<ImageMetaQueryPort>()
    private val imageMetaSavePort = mockk<ImageMetaSavePort>()
    private val service =
        ConfirmImageUploadService(
            imageMetaQueryPort = imageMetaQueryPort,
            imageMetaSavePort = imageMetaSavePort,
        )

    private fun pendingImage(): ImageMeta =
        ImageMeta.reconstitute(
            id = 77L,
            url = "https://s3.example/nukki/user1/uuid.png",
            uploadedByMemberId = 1L,
            imageType = ImageType.NUKKI,
            status = ImageStatus.PENDING,
            referenceCount = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `PENDING 이미지를 확인하면 COMPLETED로 저장하고 결과를 반환한다`() {
        every { imageMetaQueryPort.findById(77L) } returns pendingImage()
        every { imageMetaSavePort.save(any()) } answers { firstArg() }

        val result = service.confirm(ConfirmImageUploadCommand(imageId = 77L))

        result.imageId shouldBe 77L
        result.imageUrl shouldBe "https://s3.example/nukki/user1/uuid.png"
        result.status shouldBe ImageStatus.COMPLETED
    }

    @Test
    fun `존재하지 않는 imageId면 IMAGE_NOT_FOUND 예외를 던진다`() {
        every { imageMetaQueryPort.findById(999L) } returns null

        val exception =
            assertFailsWith<BusinessException> {
                service.confirm(ConfirmImageUploadCommand(imageId = 999L))
            }
        exception.errorCode shouldBe ImageErrorCode.IMAGE_NOT_FOUND
    }

    @Test
    fun `이미 COMPLETED인 이미지면 IMAGE_ALREADY_CONFIRMED 예외를 던진다`() {
        val completedImage =
            ImageMeta.reconstitute(
                id = 77L,
                url = "https://s3.example/nukki/user1/uuid.png",
                uploadedByMemberId = 1L,
                imageType = ImageType.NUKKI,
                status = ImageStatus.COMPLETED,
                referenceCount = 0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        every { imageMetaQueryPort.findById(77L) } returns completedImage

        val exception =
            assertFailsWith<BusinessException> {
                service.confirm(ConfirmImageUploadCommand(imageId = 77L))
            }
        exception.errorCode shouldBe ImageErrorCode.IMAGE_ALREADY_CONFIRMED
    }
}
