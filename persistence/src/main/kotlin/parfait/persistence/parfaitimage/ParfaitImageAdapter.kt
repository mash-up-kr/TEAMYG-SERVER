package parfait.persistence.parfaitimage

import org.springframework.stereotype.Component
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.core.parfaitimage.port.out.ParfaitImageDeletePort
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort
import parfait.persistence.repository.ParfaitImageRepository
import parfait.persistence.entity.ParfaitImage as ParfaitImageEntity

@Component
class ParfaitImageAdapter(
    private val parfaitImageRepository: ParfaitImageRepository,
) : ParfaitImageQueryPort,
    ParfaitImageSavePort,
    ParfaitImageDeletePort {
    override fun findByParfaitIdAndImageMetaId(
        parfaitId: Long,
        imageMetaId: Long,
    ): ParfaitImage? = parfaitImageRepository.findByParfaitIdAndImageMetaId(parfaitId, imageMetaId)?.toDomain()

    override fun findById(parfaitImageId: Long): ParfaitImage? =
        parfaitImageRepository.findById(parfaitImageId).orElse(null)?.toDomain()

    override fun findAllByParfaitId(parfaitId: Long): List<ParfaitImage> =
        parfaitImageRepository.findAllByParfaitId(parfaitId).map { it.toDomain() }

    override fun save(parfaitImage: ParfaitImage): ParfaitImage =
        parfaitImageRepository.save(parfaitImage.toEntity()).toDomain()

    override fun deleteById(parfaitImageId: Long) {
        parfaitImageRepository.deleteById(parfaitImageId)
    }

    private fun ParfaitImage.toEntity(): ParfaitImageEntity =
        ParfaitImageEntity(
            parfaitId = parfaitId,
            imageMetaId = imageMetaId,
            placedByGroupMemberId = placedByGroupMemberId,
            imageUrl = imageUrl,
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            scale = scale,
            rotation = rotation,
            borderType = borderType.name,
            borderColor = borderColor,
            borderWidth = borderWidth,
            createdAt = createdAt,
            updatedAt = updatedAt,
            id = id,
        )

    private fun ParfaitImageEntity.toDomain(): ParfaitImage =
        ParfaitImage.reconstitute(
            id = requireNotNull(id),
            parfaitId = parfaitId,
            imageMetaId = imageMetaId,
            placedByGroupMemberId = placedByGroupMemberId,
            imageUrl = imageUrl,
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            scale = scale,
            rotation = rotation,
            borderType = BorderType.valueOf(borderType),
            borderColor = borderColor,
            borderWidth = borderWidth,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
