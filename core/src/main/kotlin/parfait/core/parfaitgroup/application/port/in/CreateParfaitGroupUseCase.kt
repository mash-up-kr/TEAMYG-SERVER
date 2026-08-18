@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

import parfait.core.parfaitgroup.domain.NameTagChipType
import java.time.LocalDateTime

interface CreateParfaitGroupUseCase {
    fun create(command: CreateParfaitGroupCommand): CreateParfaitGroupResult
}

data class CreateParfaitGroupCommand(
    val memberId: Long,
    val groupName: String,
    val groupNickname: String,
    val memberLimit: Int,
)

data class CreateParfaitGroupResult(
    val groupId: Long,
    val groupName: String,
    val inviteCode: String,
    val memberLimit: Int,
    val recentImageUrl: String?,
    val recentImageUploadedAt: LocalDateTime,
    val lastPlacedByNametagChip: NameTagChipType,
)
