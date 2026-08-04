package parfait.core.image.domain

import java.time.LocalDateTime

class ImageMeta private constructor(
    val id: Long?,
    val url: String,
    val uploadedByMemberId: Long,
    val imageType: ImageType,
    val status: ImageStatus,
    val referenceCount: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun requireId(): Long = requireNotNull(id) { "저장된 이미지의 id가 필요합니다" }

    companion object {
        fun createPending(
            url: String,
            uploadedByMemberId: Long,
            imageType: ImageType,
            now: LocalDateTime = LocalDateTime.now(),
        ): ImageMeta =
            ImageMeta(
                id = null,
                url = url,
                uploadedByMemberId = uploadedByMemberId,
                imageType = imageType,
                status = ImageStatus.PENDING,
                referenceCount = 0,
                createdAt = now,
                updatedAt = now,
            )

        fun reconstitute(
            id: Long,
            url: String,
            uploadedByMemberId: Long,
            imageType: ImageType,
            status: ImageStatus,
            referenceCount: Long,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
        ): ImageMeta =
            ImageMeta(
                id = id,
                url = url,
                uploadedByMemberId = uploadedByMemberId,
                imageType = imageType,
                status = status,
                referenceCount = referenceCount,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }
}
