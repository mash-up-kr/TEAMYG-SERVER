package parfait.core.image.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.image.domain.ImageKeyGenerator
import parfait.core.image.domain.ImageMeta
import parfait.core.image.exception.ImageErrorCode
import parfait.core.image.port.`in`.IssueImageUploadUrlCommand
import parfait.core.image.port.`in`.IssueImageUploadUrlResult
import parfait.core.image.port.`in`.IssueImageUploadUrlUseCase
import parfait.core.image.port.out.ImageMetaSavePort
import parfait.core.image.port.out.ImageUploadUrlIssuePort
import parfait.core.member.port.out.MemberQueryPort

@Service
class IssueImageUploadUrlService(
    private val memberQueryPort: MemberQueryPort,
    private val imageUploadUrlIssuePort: ImageUploadUrlIssuePort,
    private val imageMetaSavePort: ImageMetaSavePort,
    @Value("\${aws.s3.presigned-url-expiration-seconds}") private val expirationSeconds: Long,
) : IssueImageUploadUrlUseCase {
    @Transactional
    override fun issue(command: IssueImageUploadUrlCommand): IssueImageUploadUrlResult {
        if (!memberQueryPort.existsById(command.memberId)) {
            throw BusinessException(ImageErrorCode.MEMBER_NOT_FOUND)
        }

        val key = ImageKeyGenerator.generate(command.imageType, command.contentType, command.memberId)
        val presignedUpload = imageUploadUrlIssuePort.issue(key, command.contentType, expirationSeconds)

        val savedImageMeta =
            imageMetaSavePort.save(
                ImageMeta.createPending(
                    url = presignedUpload.imageUrl,
                    uploadedByMemberId = command.memberId,
                    imageType = command.imageType,
                ),
            )

        return IssueImageUploadUrlResult(
            imageId = savedImageMeta.requireId(),
            uploadUrl = presignedUpload.uploadUrl,
            imageUrl = presignedUpload.imageUrl,
            expiresIn = expirationSeconds,
        )
    }
}
