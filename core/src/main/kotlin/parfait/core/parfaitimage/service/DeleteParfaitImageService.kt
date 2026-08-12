package parfait.core.parfaitimage.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.image.port.out.ImageDeletePort
import parfait.core.image.port.out.ImageMetaQueryPort
import parfait.core.image.port.out.ImageMetaSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.DeleteParfaitImageCommand
import parfait.core.parfaitimage.port.`in`.DeleteParfaitImageUseCase
import parfait.core.parfaitimage.port.out.ParfaitImageDeletePort
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort

@Service
class DeleteParfaitImageService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitImageQueryPort: ParfaitImageQueryPort,
    private val parfaitImageDeletePort: ParfaitImageDeletePort,
    private val imageMetaQueryPort: ImageMetaQueryPort,
    private val imageMetaSavePort: ImageMetaSavePort,
    private val imageDeletePort: ImageDeletePort,
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
