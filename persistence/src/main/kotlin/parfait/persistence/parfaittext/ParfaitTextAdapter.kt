package parfait.persistence.parfaittext

import org.springframework.stereotype.Component
import parfait.core.parfaittext.domain.ParfaitText
import parfait.core.parfaittext.port.out.ParfaitTextQueryPort
import parfait.core.parfaittext.port.out.ParfaitTextSavePort
import parfait.persistence.repository.ParfaitTextRepository
import parfait.persistence.entity.ParfaitText as ParfaitTextEntity

@Component
class ParfaitTextAdapter(
    private val parfaitTextRepository: ParfaitTextRepository,
) : ParfaitTextQueryPort,
    ParfaitTextSavePort {
    override fun findAllByParfaitId(parfaitId: Long): List<ParfaitText> =
        parfaitTextRepository.findAllByParfaitIdOrderByPositionZAscIdAsc(parfaitId).map { it.toDomain() }

    override fun existsByParfaitId(parfaitId: Long): Boolean = parfaitTextRepository.existsByParfaitId(parfaitId)

    override fun save(parfaitText: ParfaitText): ParfaitText =
        parfaitTextRepository
            .save(
                ParfaitTextEntity(
                    parfaitId = parfaitText.parfaitId,
                    placedByGroupMemberId = parfaitText.placedByGroupMemberId,
                    text = parfaitText.text,
                    positionX = parfaitText.positionX,
                    positionY = parfaitText.positionY,
                    positionZ = parfaitText.positionZ,
                    scale = parfaitText.scale,
                    rotation = parfaitText.rotation,
                    color = parfaitText.color,
                    fontSize = parfaitText.fontSize,
                    createdAt = parfaitText.createdAt,
                    updatedAt = parfaitText.updatedAt,
                    id = parfaitText.id,
                ),
            ).toDomain()

    private fun ParfaitTextEntity.toDomain(): ParfaitText =
        ParfaitText.reconstitute(
            id = requireNotNull(id),
            parfaitId = parfaitId,
            placedByGroupMemberId = placedByGroupMemberId,
            text = text,
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            scale = scale,
            rotation = rotation,
            color = color,
            fontSize = fontSize,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
