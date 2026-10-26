package parfait.persistence.parfaittext

import org.springframework.stereotype.Component
import parfait.core.parfaittext.domain.ParfaitText
import parfait.core.parfaittext.domain.TextAlign
import parfait.core.parfaittext.port.out.ParfaitTextDeletePort
import parfait.core.parfaittext.port.out.ParfaitTextQueryPort
import parfait.core.parfaittext.port.out.ParfaitTextSavePort
import parfait.persistence.repository.ParfaitTextRepository
import parfait.persistence.entity.ParfaitText as ParfaitTextEntity

@Component
class ParfaitTextAdapter(
    private val parfaitTextRepository: ParfaitTextRepository,
) : ParfaitTextQueryPort,
    ParfaitTextSavePort,
    ParfaitTextDeletePort {
    override fun findById(parfaitTextId: Long): ParfaitText? =
        parfaitTextRepository.findById(parfaitTextId).orElse(null)?.toDomain()

    override fun deleteById(parfaitTextId: Long) {
        parfaitTextRepository.deleteById(parfaitTextId)
    }

    override fun findAllByIds(parfaitTextIds: List<Long>): List<ParfaitText> =
        parfaitTextRepository.findAllById(parfaitTextIds).map { it.toDomain() }

    override fun findAllByParfaitId(parfaitId: Long): List<ParfaitText> =
        parfaitTextRepository.findAllByParfaitIdOrderByPositionZAscIdAsc(parfaitId).map { it.toDomain() }

    override fun existsByParfaitId(parfaitId: Long): Boolean = parfaitTextRepository.existsByParfaitId(parfaitId)

    override fun save(parfaitText: ParfaitText): ParfaitText =
        parfaitTextRepository.save(parfaitText.toEntity()).toDomain()

    override fun saveAll(parfaitTexts: List<ParfaitText>): List<ParfaitText> =
        parfaitTextRepository.saveAll(parfaitTexts.map { it.toEntity() }).map { it.toDomain() }

    private fun ParfaitText.toEntity(): ParfaitTextEntity =
        ParfaitTextEntity(
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
            isFill = isFill,
            isStroke = isStroke,
            textAlign = textAlign.name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            id = id,
        )

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
            isFill = isFill,
            isStroke = isStroke,
            textAlign = TextAlign.valueOf(textAlign),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
