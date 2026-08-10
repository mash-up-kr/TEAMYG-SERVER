package parfait.http.parfaitimage.dto

import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageResult

data class UpdateParfaitImageResponse(
    val parfaitImageId: Long,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
) {
    companion object {
        fun from(result: UpdateParfaitImageResult): UpdateParfaitImageResponse =
            UpdateParfaitImageResponse(
                parfaitImageId = result.parfaitImageId,
                positionX = result.positionX,
                positionY = result.positionY,
                positionZ = result.positionZ,
                scale = result.scale,
                rotation = result.rotation,
            )
    }
}
