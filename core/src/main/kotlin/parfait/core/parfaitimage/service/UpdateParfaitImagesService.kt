package parfait.core.parfaitimage.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.service.ParfaitVersionService
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageResult
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesResult
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesUseCase
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort
import parfait.core.parfaittext.exception.ParfaitTextErrorCode
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextResult
import parfait.core.parfaittext.port.out.ParfaitTextQueryPort
import parfait.core.parfaittext.port.out.ParfaitTextSavePort

@Service
class UpdateParfaitImagesService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitImageQueryPort: ParfaitImageQueryPort,
    private val parfaitImageSavePort: ParfaitImageSavePort,
    private val parfaitTextQueryPort: ParfaitTextQueryPort,
    private val parfaitTextSavePort: ParfaitTextSavePort,
    private val parfaitVersionService: ParfaitVersionService,
) : UpdateParfaitImagesUseCase {
    @Transactional
    override fun updateAll(command: UpdateParfaitImagesCommand): UpdateParfaitImagesResult {
        if (command.items.isEmpty() && command.textItems.isEmpty()) {
            return UpdateParfaitImagesResult(images = emptyList(), texts = emptyList())
        }

        val groupMember =
            parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(command.groupId, command.memberId)
                ?: throw BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED)

        val parfait =
            parfaitQueryPort.findByIdAndGroupId(command.parfaitId, command.groupId)
                ?: throw BusinessException(ParfaitImageErrorCode.PARFAIT_NOT_FOUND)
        if (parfait.status != ParfaitStatus.ACTIVE) {
            throw BusinessException(ParfaitErrorCode.PARFAIT_ALREADY_CLOSED)
        }

        val existingById =
            parfaitImageQueryPort
                .findAllByIds(command.items.map { it.parfaitImageId })
                .associateBy { it.requireId() }

        val toSave =
            command.items.map { item ->
                val parfaitImage =
                    existingById[item.parfaitImageId]
                        ?.takeIf { it.parfaitId == command.parfaitId }
                        ?: throw BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_FOUND)

                if (groupMember.id != parfaitImage.placedByGroupMemberId) {
                    throw BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED)
                }

                parfaitImage.update(
                    positionX = item.positionX,
                    positionY = item.positionY,
                    positionZ = item.positionZ,
                    scale = item.scale,
                    rotation = item.rotation,
                )
            }

        val existingTextById =
            parfaitTextQueryPort
                .findAllByIds(command.textItems.map { it.parfaitTextId })
                .associateBy { it.requireId() }

        val textsToSave =
            command.textItems.map { item ->
                val parfaitText =
                    existingTextById[item.parfaitTextId]
                        ?.takeIf { it.parfaitId == command.parfaitId }
                        ?: throw BusinessException(ParfaitTextErrorCode.PARFAIT_TEXT_NOT_FOUND)

                if (groupMember.id != parfaitText.placedByGroupMemberId) {
                    throw BusinessException(ParfaitTextErrorCode.PARFAIT_TEXT_NOT_OWNED)
                }

                parfaitText.update(
                    positionX = item.positionX,
                    positionY = item.positionY,
                    positionZ = item.positionZ,
                    scale = item.scale,
                    rotation = item.rotation,
                )
            }

        // 부모 parfait 행 X 락을 자식(parfait_image) 변경보다 먼저 잡는다.
        // 배치(Place) 등 다른 쓰기 경로가 "parfait → parfait_image" 순서로 락을 잡으므로
        // 순서를 통일하지 않으면 동시 요청 시 교차 대기로 데드락이 날 수 있다.
        parfaitVersionService.bump(command.parfaitId)

        val savedImages = parfaitImageSavePort.saveAll(toSave)
        val savedTexts = parfaitTextSavePort.saveAll(textsToSave)

        return UpdateParfaitImagesResult(
            images =
                savedImages.map { saved ->
                    UpdateParfaitImageResult(
                        parfaitImageId = saved.requireId(),
                        positionX = saved.positionX,
                        positionY = saved.positionY,
                        positionZ = saved.positionZ,
                        scale = saved.scale,
                        rotation = saved.rotation,
                    )
                },
            texts =
                savedTexts.map { saved ->
                    UpdateParfaitTextResult(
                        parfaitTextId = saved.requireId(),
                        text = saved.text,
                        positionX = saved.positionX,
                        positionY = saved.positionY,
                        positionZ = saved.positionZ,
                        scale = saved.scale,
                        rotation = saved.rotation,
                        color = saved.color,
                        fontSize = saved.fontSize,
                        isFill = saved.isFill,
                        isStroke = saved.isStroke,
                    )
                },
        )
    }
}
