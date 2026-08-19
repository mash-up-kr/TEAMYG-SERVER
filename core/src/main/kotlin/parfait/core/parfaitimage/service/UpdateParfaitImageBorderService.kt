package parfait.core.parfaitimage.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderResult
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderUseCase
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort

@Service
class UpdateParfaitImageBorderService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitImageQueryPort: ParfaitImageQueryPort,
    private val parfaitImageSavePort: ParfaitImageSavePort,
) : UpdateParfaitImageBorderUseCase {
    @Transactional
    override fun update(command: UpdateParfaitImageBorderCommand): UpdateParfaitImageBorderResult {
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

        val saved =
            parfaitImageSavePort.save(
                parfaitImage.updateBorder(
                    borderType = command.borderType,
                    borderColor = command.borderColor,
                    borderWidth = command.borderWidth,
                ),
            )

        return UpdateParfaitImageBorderResult(
            parfaitImageId = saved.requireId(),
            borderType = saved.borderType,
            borderColor = saved.borderColor,
            borderWidth = saved.borderWidth,
        )
    }
}
