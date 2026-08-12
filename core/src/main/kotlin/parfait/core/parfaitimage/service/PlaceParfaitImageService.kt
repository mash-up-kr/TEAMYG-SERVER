package parfait.core.parfaitimage.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.image.domain.ImageStatus
import parfait.core.image.exception.ImageErrorCode
import parfait.core.image.port.out.ImageMetaQueryPort
import parfait.core.image.port.out.ImageMetaSavePort
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageCommand
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageResult
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageUseCase
import parfait.core.parfaitimage.port.`in`.PlacedByResult
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
) : PlaceParfaitImageUseCase {
    @Transactional
    override fun place(command: PlaceParfaitImageCommand): PlaceParfaitImageResult {
        val groupMember =
            parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(command.groupId, command.memberId)
                ?: throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

        if (!parfaitQueryPort.existsByIdAndGroupId(command.parfaitId, command.groupId)) {
            throw BusinessException(ParfaitImageErrorCode.PARFAIT_NOT_FOUND)
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

        val saved = parfaitImageSavePort.save(toSave)

        if (existing == null) {
            imageMetaSavePort.save(imageMeta.incrementReferenceCount())
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
                PlacedByResult(
                    groupMemberId = requireNotNull(groupMember.id),
                    nickname = groupMember.groupNickname.value,
                ),
        )
    }
}
