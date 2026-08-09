package parfait.persistence.image

import org.springframework.stereotype.Component
import parfait.core.image.domain.ImageMeta
import parfait.core.image.domain.ImageStatus
import parfait.core.image.domain.ImageType
import parfait.core.image.port.out.ImageMetaQueryPort
import parfait.core.image.port.out.ImageMetaSavePort
import parfait.persistence.repository.ImageMetaRepository
import parfait.persistence.entity.ImageMeta as ImageMetaEntity

@Component
class ImageMetaAdapter(
    private val imageMetaRepository: ImageMetaRepository,
) : ImageMetaSavePort,
    ImageMetaQueryPort {
    override fun save(imageMeta: ImageMeta): ImageMeta = imageMetaRepository.save(imageMeta.toEntity()).toDomain()

    override fun findById(imageId: Long): ImageMeta? = imageMetaRepository.findById(imageId).orElse(null)?.toDomain()

    private fun ImageMeta.toEntity(): ImageMetaEntity =
        ImageMetaEntity(
            url = url,
            uploadedByMemberId = uploadedByMemberId,
            imageType = imageType.name,
            status = status.name,
            referenceCount = referenceCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            id = id,
        )

    private fun ImageMetaEntity.toDomain(): ImageMeta =
        ImageMeta.reconstitute(
            id = requireNotNull(id),
            url = url,
            uploadedByMemberId = uploadedByMemberId,
            imageType = ImageType.valueOf(imageType),
            status = ImageStatus.valueOf(status),
            referenceCount = referenceCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
