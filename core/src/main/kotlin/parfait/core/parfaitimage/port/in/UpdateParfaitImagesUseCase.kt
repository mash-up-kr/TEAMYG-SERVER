@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitimage.port.`in`

import parfait.core.parfaittext.port.`in`.UpdateParfaitTextResult

interface UpdateParfaitImagesUseCase {
    fun updateAll(command: UpdateParfaitImagesCommand): UpdateParfaitImagesResult
}

data class UpdateParfaitImagesCommand(
    val memberId: Long,
    val groupId: Long,
    val parfaitId: Long,
    val items: List<UpdateParfaitImageItemCommand>,
    val textItems: List<UpdateParfaitTextItemCommand> = emptyList(),
)

data class UpdateParfaitImagesResult(
    val images: List<UpdateParfaitImageResult>,
    val texts: List<UpdateParfaitTextResult>,
)

data class UpdateParfaitImageItemCommand(
    val parfaitImageId: Long,
    val positionX: Double?,
    val positionY: Double?,
    val positionZ: Int?,
    val scale: Double?,
    val rotation: Double?,
)

data class UpdateParfaitTextItemCommand(
    val parfaitTextId: Long,
    val positionX: Double?,
    val positionY: Double?,
    val positionZ: Int?,
    val scale: Double?,
    val rotation: Double?,
)
