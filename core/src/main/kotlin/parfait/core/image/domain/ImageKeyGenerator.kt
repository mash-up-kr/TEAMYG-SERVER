package parfait.core.image.domain

import parfait.core.exception.BusinessException
import parfait.core.image.exception.ImageErrorCode
import java.util.UUID

object ImageKeyGenerator {
    fun generate(
        imageType: ImageType,
        contentType: String,
        memberId: Long,
    ): String {
        val extension = extensionOf(contentType)
        return "${imageType.name.lowercase()}/user$memberId/${UUID.randomUUID()}.$extension"
    }

    private fun extensionOf(contentType: String): String =
        when (contentType) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            else -> throw BusinessException(ImageErrorCode.INVALID_CONTENT_TYPE)
        }
}
