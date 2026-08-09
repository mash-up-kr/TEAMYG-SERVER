package parfait.http.image.dto

import parfait.core.image.port.`in`.ConfirmImageUploadResult

data class ConfirmImageUploadResponse(
    val imageId: Long,
    val imageUrl: String,
    val status: String,
) {
    companion object {
        fun from(result: ConfirmImageUploadResult): ConfirmImageUploadResponse =
            ConfirmImageUploadResponse(
                imageId = result.imageId,
                imageUrl = result.imageUrl,
                status = result.status.name,
            )
    }
}
