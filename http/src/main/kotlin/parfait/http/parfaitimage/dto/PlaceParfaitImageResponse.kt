package parfait.http.parfaitimage.dto

import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImagePlacedByResult
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageResult

data class PlaceParfaitImageResponse(
    val parfaitImageId: Long,
    val imageId: Long,
    val imageUrl: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val placedBy: PlaceParfaitImagePlacedByResponse,
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
                placedBy = PlaceParfaitImagePlacedByResponse.from(result.placedBy),
            )
    }
}

data class PlaceParfaitImagePlacedByResponse(
    val groupMemberId: Long,
    val nickname: String,
    val nameTagChip: NameTagChipType,
) {
    companion object {
        fun from(result: PlaceParfaitImagePlacedByResult): PlaceParfaitImagePlacedByResponse =
            PlaceParfaitImagePlacedByResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
                nameTagChip = result.nametagChip,
            )
    }
}
