@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitimage.port.`in`

import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitimage.domain.BorderType

interface PlaceParfaitImageUseCase {
    fun place(command: PlaceParfaitImageCommand): PlaceParfaitImageResult
}

data class PlaceParfaitImageCommand(
    val memberId: Long,
    val groupId: Long,
    val parfaitId: Long,
    val imageId: Long,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val borderType: BorderType,
    val borderColor: String?,
    val borderWidth: Double?,
)

data class PlaceParfaitImageResult(
    val parfaitImageId: Long,
    val imageId: Long,
    val imageUrl: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val placedBy: PlaceParfaitImagePlacedByResult,
)

data class PlaceParfaitImagePlacedByResult(
    val groupMemberId: Long,
    val nickname: String,
    val nametagChip: NameTagChipType,
)
