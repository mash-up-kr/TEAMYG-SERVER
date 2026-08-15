@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfait.port.`in`

import parfait.core.parfait.domain.BackgroundType

interface ChangeParfaitBackgroundUseCase {
    fun change(command: ChangeParfaitBackgroundCommand): BackgroundResult
}

data class ChangeParfaitBackgroundCommand(
    val memberId: Long,
    val groupId: Long,
    val parfaitId: Long,
    val type: BackgroundType,
    val value: String?,
    val imageId: Long?,
)
