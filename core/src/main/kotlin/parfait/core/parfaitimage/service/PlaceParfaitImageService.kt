package parfait.core.parfaitimage.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.image.domain.ImageStatus
import parfait.core.image.exception.ImageErrorCode
import parfait.core.image.port.out.ImageMetaQueryPort
import parfait.core.image.port.out.ImageMetaSavePort
import parfait.core.notification.domain.ToppingPlacedPayload
import parfait.core.notification.service.ToppingPlacedNotifier
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.service.ParfaitVersionService
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageCommand
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImagePlacedByResult
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageResult
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageUseCase
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort

@Service
class PlaceParfaitImageService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val imageMetaQueryPort: ImageMetaQueryPort,
    private val imageMetaSavePort: ImageMetaSavePort,
    private val parfaitImageQueryPort: ParfaitImageQueryPort,
    private val parfaitImageSavePort: ParfaitImageSavePort,
    private val toppingPlacedNotifier: ToppingPlacedNotifier,
    private val parfaitVersionService: ParfaitVersionService,
) : PlaceParfaitImageUseCase {
    @Transactional
    override fun place(command: PlaceParfaitImageCommand): PlaceParfaitImageResult {
        val groupMember =
            parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(command.groupId, command.memberId)
                ?: throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

        val parfait =
            parfaitQueryPort.findByIdAndGroupId(command.parfaitId, command.groupId)
                ?: throw BusinessException(ParfaitImageErrorCode.PARFAIT_NOT_FOUND)
        if (parfait.status != ParfaitStatus.ACTIVE) {
            throw BusinessException(ParfaitErrorCode.PARFAIT_ALREADY_CLOSED)
        }

        val imageMeta =
            imageMetaQueryPort.findById(command.imageId)
                ?: throw BusinessException(ImageErrorCode.IMAGE_NOT_FOUND)
        if (imageMeta.status != ImageStatus.COMPLETED) {
            throw BusinessException(ParfaitImageErrorCode.IMAGE_NOT_CONFIRMED)
        }

        val existing = parfaitImageQueryPort.findByParfaitIdAndImageMetaId(command.parfaitId, command.imageId)
        val toSave =
            existing?.reposition(
                placedByGroupMemberId = requireNotNull(groupMember.id),
                positionX = command.positionX,
                positionY = command.positionY,
                positionZ = command.positionZ,
                scale = command.scale,
                rotation = command.rotation,
                borderType = command.borderType,
                borderColor = command.borderColor,
                borderWidth = command.borderWidth,
            ) ?: ParfaitImage.place(
                parfaitId = command.parfaitId,
                imageMetaId = command.imageId,
                placedByGroupMemberId = requireNotNull(groupMember.id),
                imageUrl = imageMeta.url,
                positionX = command.positionX,
                positionY = command.positionY,
                positionZ = command.positionZ,
                scale = command.scale,
                rotation = command.rotation,
                borderType = command.borderType,
                borderColor = command.borderColor,
                borderWidth = command.borderWidth,
            )

        // version 증가(부모 parfait 행 UPDATE)는 반드시 parfait_image INSERT 보다 먼저 해야 한다.
        // parfait_image.parfait_id 의 FK 검사 때문에 INSERT 는 부모 parfait 행에 공유 락(S)을 건다.
        // INSERT 뒤에 같은 행을 UPDATE 하면 S -> X 승격이 필요해, 같은 캔버스에 두 멤버가 동시에 새 이미지를 배치하면
        // 두 트랜잭션이 서로의 S 락을 기다리며 데드락(MySQL 1213)이 난다.
        // 부모 행의 배타 락(X)을 먼저 잡으면 같은 캔버스의 쓰기가 이 지점에서 직렬화되어 승격 자체가 사라진다.
        // 검증(도메인 검증 포함)이 모두 끝난 뒤에 호출해 실패한 요청이 락을 잡거나 version 을 올리지 않게 한다.
        parfaitVersionService.bump(command.parfaitId)
        val saved = parfaitImageSavePort.save(toSave)

        if (existing == null) {
            imageMetaSavePort.save(imageMeta.incrementReferenceCount())
            toppingPlacedNotifier.notify(
                payload =
                    ToppingPlacedPayload(
                        groupId = command.groupId,
                        parfaitId = command.parfaitId,
                        parfaitDate = parfait.parfaitDate,
                        actorMemberId = command.memberId,
                    ),
                toppingId = saved.requireId(),
            )
        }

        return PlaceParfaitImageResult(
            parfaitImageId = saved.requireId(),
            imageId = saved.imageMetaId,
            imageUrl = saved.imageUrl,
            positionX = saved.positionX,
            positionY = saved.positionY,
            positionZ = saved.positionZ,
            scale = saved.scale,
            rotation = saved.rotation,
            placedBy =
                PlaceParfaitImagePlacedByResult(
                    groupMemberId = requireNotNull(groupMember.id),
                    nickname = groupMember.groupNickname.value,
                    nametagChip = groupMember.nametagChip,
                ),
        )
    }
}
