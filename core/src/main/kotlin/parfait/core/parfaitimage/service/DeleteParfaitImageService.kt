package parfait.core.parfaitimage.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.image.port.out.ImageDeletePort
import parfait.core.image.port.out.ImageMetaQueryPort
import parfait.core.image.port.out.ImageMetaSavePort
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.service.ParfaitVersionService
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.DeleteParfaitImageCommand
import parfait.core.parfaitimage.port.`in`.DeleteParfaitImageUseCase
import parfait.core.parfaitimage.port.out.ParfaitImageDeletePort
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort

@Service
class DeleteParfaitImageService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitImageQueryPort: ParfaitImageQueryPort,
    private val parfaitImageDeletePort: ParfaitImageDeletePort,
    private val imageMetaQueryPort: ImageMetaQueryPort,
    private val imageMetaSavePort: ImageMetaSavePort,
    private val imageDeletePort: ImageDeletePort,
    private val parfaitVersionService: ParfaitVersionService,
) : DeleteParfaitImageUseCase {
    @Transactional
    override fun delete(command: DeleteParfaitImageCommand) {
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

        parfaitImageDeletePort.deleteById(parfaitImage.requireId())

        val imageMeta =
            requireNotNull(imageMetaQueryPort.findById(parfaitImage.imageMetaId)) {
                "parfait_image가 참조하는 image_meta는 FK로 항상 존재해야 합니다"
            }
        val decremented = imageMeta.decrementReferenceCount()
        imageMetaSavePort.save(decremented)

        if (decremented.referenceCount == 0L) {
            imageDeletePort.delete(decremented.url)
        }
    }
}
