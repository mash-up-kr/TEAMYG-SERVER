package parfait.core.parfait.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.image.domain.ImageStatus
import parfait.core.image.exception.ImageErrorCode
import parfait.core.image.port.out.ImageMetaQueryPort
import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.BackgroundResult
import parfait.core.parfait.port.`in`.ChangeParfaitBackgroundCommand
import parfait.core.parfait.port.`in`.ChangeParfaitBackgroundUseCase
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException

@Service
class ChangeParfaitBackgroundService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitSavePort: ParfaitSavePort,
    private val imageMetaQueryPort: ImageMetaQueryPort,
) : ChangeParfaitBackgroundUseCase {
    @Transactional
    override fun change(command: ChangeParfaitBackgroundCommand): BackgroundResult {
        if (!parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(command.groupId, command.memberId)) {
            throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)
        }

        val parfait =
            parfaitQueryPort.findByIdAndGroupId(command.parfaitId, command.groupId)
                ?: throw BusinessException(ParfaitErrorCode.PARFAIT_NOT_FOUND)

        val resolvedValue = resolveValue(command)

        val saved = parfaitSavePort.save(parfait.changeBackground(command.type, resolvedValue))

        return BackgroundResult(
            type = requireNotNull(saved.backgroundType),
            value = requireNotNull(saved.backgroundValue),
        )
    }

    private fun resolveValue(command: ChangeParfaitBackgroundCommand): String =
        when (command.type) {
            BackgroundType.COLOR -> command.value ?: throw BusinessException(ParfaitErrorCode.INVALID_BACKGROUND)
            BackgroundType.IMAGE -> {
                val imageId = command.imageId ?: throw BusinessException(ParfaitErrorCode.INVALID_BACKGROUND)
                val imageMeta =
                    imageMetaQueryPort.findById(imageId)
                        ?: throw BusinessException(ImageErrorCode.IMAGE_NOT_FOUND)
                if (imageMeta.status != ImageStatus.COMPLETED) {
                    throw BusinessException(ParfaitErrorCode.BACKGROUND_IMAGE_NOT_CONFIRMED)
                }
                imageMeta.url
            }
        }
}
