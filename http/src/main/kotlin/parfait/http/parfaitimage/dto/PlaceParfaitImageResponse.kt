package parfait.http.parfaitimage.dto

import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageResult
import parfait.core.parfaitimage.port.`in`.PlacedByResult

data class PlaceParfaitImageResponse(
    val parfaitImageId: Long,
    val imageId: Long,
    val imageUrl: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val placedBy: PlacedByResponse,
) {
    companion object {
        fun from(result: PlaceParfaitImageResult): PlaceParfaitImageResponse =
            PlaceParfaitImageResponse(
                parfaitImageId = result.parfaitImageId,
                imageId = result.imageId,
                imageUrl = result.imageUrl,
                positionX = result.positionX,
                positionY = result.positionY,
                positionZ = result.positionZ,
                scale = result.scale,
                rotation = result.rotation,
                placedBy = PlacedByResponse.from(result.placedBy),
            )
    }
}

data class PlacedByResponse(
    val groupMemberId: Long,
    val nickname: String,
) {
    companion object {
        fun from(result: PlacedByResult): PlacedByResponse =
            PlacedByResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
            )
    }
}
