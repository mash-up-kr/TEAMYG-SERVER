@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitimage.port.`in`

interface UpdateParfaitImageUseCase {
    fun update(command: UpdateParfaitImageCommand): UpdateParfaitImageResult
}

data class UpdateParfaitImageCommand(
    val memberId: Long,
    val groupId: Long,
    val parfaitId: Long,
    val parfaitImageId: Long,
    val positionX: Double?,
    val positionY: Double?,
    val positionZ: Int?,
    val scale: Double?,
    val rotation: Double?,
)

data class UpdateParfaitImageResult(
    val parfaitImageId: Long,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
)
