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
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageResult
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageUseCase
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort

@Service
class UpdateParfaitImageService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitImageQueryPort: ParfaitImageQueryPort,
    private val parfaitImageSavePort: ParfaitImageSavePort,
    private val parfaitVersionService: ParfaitVersionService,
) : UpdateParfaitImageUseCase {
    @Transactional
    override fun update(command: UpdateParfaitImageCommand): UpdateParfaitImageResult {
        val parfaitImage =
            parfaitImageQueryPort
                .findById(command.parfaitImageId)
                ?.takeIf { it.parfaitId == command.parfaitId }
                ?: throw BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_FOUND)

        val groupMember = parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(command.groupId, command.memberId)
        if (groupMember == null || groupMember.id != parfaitImage.placedByGroupMemberId) {
            throw BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED)
        }

        val parfait =
            parfaitQueryPort.findByIdAndGroupId(command.parfaitId, command.groupId)
                ?: throw BusinessException(ParfaitImageErrorCode.PARFAIT_NOT_FOUND)
        if (parfait.status != ParfaitStatus.ACTIVE) {
            throw BusinessException(ParfaitErrorCode.PARFAIT_ALREADY_CLOSED)
        }

        // 부모 parfait 행 X 락을 자식(parfait_image) 변경보다 먼저 잡는다.
        // 배치(Place) 등 다른 쓰기 경로가 "parfait → parfait_image" 순서로 락을 잡으므로
        // 순서를 통일하지 않으면 동시 요청 시 교차 대기로 데드락이 날 수 있다.
        parfaitVersionService.bump(command.parfaitId)

        val saved =
            parfaitImageSavePort.save(
                parfaitImage.update(
                    positionX = command.positionX,
                    positionY = command.positionY,
                    positionZ = command.positionZ,
                    scale = command.scale,
                    rotation = command.rotation,
                ),
            )

        return UpdateParfaitImageResult(
            parfaitImageId = saved.requireId(),
            positionX = saved.positionX,
            positionY = saved.positionY,
            positionZ = saved.positionZ,
            scale = saved.scale,
            rotation = saved.rotation,
        )
    }
}
