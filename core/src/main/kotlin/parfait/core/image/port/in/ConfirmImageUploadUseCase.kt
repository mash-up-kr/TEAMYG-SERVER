@file:Suppress("ktlint:standard:package-name")

package parfait.core.image.port.`in`

import parfait.core.image.domain.ImageStatus

interface ConfirmImageUploadUseCase {
    fun confirm(command: ConfirmImageUploadCommand): ConfirmImageUploadResult
}

data class ConfirmImageUploadCommand(
    val imageId: Long,
)

data class ConfirmImageUploadResult(
    val imageId: Long,
    val imageUrl: String,
    val status: ImageStatus,
)
