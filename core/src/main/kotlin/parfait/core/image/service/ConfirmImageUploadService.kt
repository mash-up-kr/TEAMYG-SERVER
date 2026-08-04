package parfait.core.image.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.image.exception.ImageErrorCode
import parfait.core.image.port.`in`.ConfirmImageUploadCommand
import parfait.core.image.port.`in`.ConfirmImageUploadResult
import parfait.core.image.port.`in`.ConfirmImageUploadUseCase
import parfait.core.image.port.out.ImageMetaQueryPort
import parfait.core.image.port.out.ImageMetaSavePort

@Service
class ConfirmImageUploadService(
    private val imageMetaQueryPort: ImageMetaQueryPort,
    private val imageMetaSavePort: ImageMetaSavePort,
) : ConfirmImageUploadUseCase {
    @Transactional
    override fun confirm(command: ConfirmImageUploadCommand): ConfirmImageUploadResult {
        val imageMeta =
            imageMetaQueryPort.findById(command.imageId)
                ?: throw BusinessException(ImageErrorCode.IMAGE_NOT_FOUND)
        val confirmed = imageMetaSavePort.save(imageMeta.confirm())
        return ConfirmImageUploadResult(
            imageId = confirmed.requireId(),
            imageUrl = confirmed.url,
            status = confirmed.status,
        )
    }
}
