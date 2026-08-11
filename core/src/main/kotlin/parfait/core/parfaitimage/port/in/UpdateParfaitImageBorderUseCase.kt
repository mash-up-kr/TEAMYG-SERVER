@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitimage.port.`in`

import parfait.core.parfaitimage.domain.BorderType

interface UpdateParfaitImageBorderUseCase {
    fun update(command: UpdateParfaitImageBorderCommand): UpdateParfaitImageBorderResult
}

data class UpdateParfaitImageBorderCommand(
    val memberId: Long,
    val groupId: Long,
    val parfaitId: Long,
    val parfaitImageId: Long,
    val borderType: BorderType,
    val borderColor: String?,
    val borderWidth: Double?,
)

data class UpdateParfaitImageBorderResult(
    val parfaitImageId: Long,
    val borderType: BorderType,
    val borderColor: String?,
    val borderWidth: Double?,
)
