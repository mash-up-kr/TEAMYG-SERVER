package parfait.core.image.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.image.domain.ImageMeta
import parfait.core.image.domain.ImageType
import parfait.core.image.exception.ImageErrorCode
import parfait.core.image.port.`in`.IssueImageUploadUrlCommand
import parfait.core.image.port.`in`.IssueImageUploadUrlResult
import parfait.core.image.port.out.ImageMetaSavePort
import parfait.core.image.port.out.ImageUploadUrlIssuePort
import parfait.core.image.port.out.PresignedUpload
import parfait.core.member.port.out.MemberQueryPort
import kotlin.test.assertFailsWith

class IssueImageUploadUrlServiceTest {
    private val memberQueryPort = mockk<MemberQueryPort>()
    private val imageUploadUrlIssuePort = mockk<ImageUploadUrlIssuePort>()
    private val imageMetaSavePort = mockk<ImageMetaSavePort>()
    private val service =
        IssueImageUploadUrlService(
            memberQueryPort = memberQueryPort,
            imageUploadUrlIssuePort = imageUploadUrlIssuePort,
            imageMetaSavePort = imageMetaSavePort,
            expirationSeconds = 900,
        )

    @Test
    fun `회원이 존재하면 S3 key를 발급받아 PENDING 상태로 저장하고 결과를 반환한다`() {
        every { memberQueryPort.existsById(42L) } returns true
        every { imageUploadUrlIssuePort.issue(any(), "image/png", 900) } returns
            PresignedUpload(uploadUrl = "https://s3.example/upload?sig", imageUrl = "https://s3.example/key.png")
        every { imageMetaSavePort.save(any()) } answers {
            val saved = firstArg<ImageMeta>()
            ImageMeta.reconstitute(
                id = 77L,
                url = saved.url,
                uploadedByMemberId = saved.uploadedByMemberId,
                imageType = saved.imageType,
                status = saved.status,
                referenceCount = saved.referenceCount,
                createdAt = saved.createdAt,
                updatedAt = saved.updatedAt,
            )
        }

        val result =
            service.issue(
                IssueImageUploadUrlCommand(memberId = 42L, contentType = "image/png", imageType = ImageType.NUKKI),
            )

        result shouldBe
            IssueImageUploadUrlResult(
                imageId = 77L,
                uploadUrl = "https://s3.example/upload?sig",
                imageUrl = "https://s3.example/key.png",
                expiresIn = 900,
            )
        verify { imageUploadUrlIssuePort.issue(match { it.startsWith("nukki/user42/") }, "image/png", 900) }
    }

    @Test
    fun `존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외를 던진다`() {
        every { memberQueryPort.existsById(999L) } returns false

        val exception =
            assertFailsWith<BusinessException> {
                service.issue(
                    IssueImageUploadUrlCommand(
                        memberId = 999L,
                        contentType = "image/png",
                        imageType = ImageType.NUKKI,
                    ),
                )
            }
        exception.errorCode shouldBe ImageErrorCode.MEMBER_NOT_FOUND
    }

    @Test
    fun `지원하지 않는 contentType이면 INVALID_CONTENT_TYPE 예외를 던진다`() {
        every { memberQueryPort.existsById(42L) } returns true

        val exception =
            assertFailsWith<BusinessException> {
                service.issue(
                    IssueImageUploadUrlCommand(
                        memberId = 42L,
                        contentType = "image/gif",
                        imageType = ImageType.NUKKI,
                    ),
                )
            }
        exception.errorCode shouldBe ImageErrorCode.INVALID_CONTENT_TYPE
    }
}
