package parfait.core.parfaitimage.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageResult
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesUseCase
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort

@Service
class UpdateParfaitImagesService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitImageQueryPort: ParfaitImageQueryPort,
    private val parfaitImageSavePort: ParfaitImageSavePort,
) : UpdateParfaitImagesUseCase {
    @Transactional
    override fun updateAll(command: UpdateParfaitImagesCommand): List<UpdateParfaitImageResult> {
        if (command.items.isEmpty()) return emptyList()

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

        return parfaitImageSavePort.saveAll(toSave).map { saved ->
            UpdateParfaitImageResult(
                parfaitImageId = saved.requireId(),
                positionX = saved.positionX,
                positionY = saved.positionY,
                positionZ = saved.positionZ,
                scale = saved.scale,
                rotation = saved.rotation,
            )
        }
    }
}
