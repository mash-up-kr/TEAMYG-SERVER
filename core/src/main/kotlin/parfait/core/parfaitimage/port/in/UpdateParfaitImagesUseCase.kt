@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitimage.port.`in`

interface UpdateParfaitImagesUseCase {
    fun updateAll(command: UpdateParfaitImagesCommand): List<UpdateParfaitImageResult>
}

data class UpdateParfaitImagesCommand(
    val memberId: Long,
    val groupId: Long,
    val parfaitId: Long,
    val items: List<UpdateParfaitImageItemCommand>,
)

data class UpdateParfaitImageItemCommand(
    val parfaitImageId: Long,
    val positionX: Double?,
    val positionY: Double?,
    val positionZ: Int?,
    val scale: Double?,
    val rotation: Double?,
)
