@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfait.port.`in`

import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.domain.OwnerType
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitimage.domain.BorderType
import java.time.LocalDate
import java.time.LocalDateTime

interface GetTodayParfaitUseCase {
    fun get(command: GetTodayParfaitCommand): GetTodayParfaitResult
}

data class GetTodayParfaitCommand(
    val memberId: Long,
    val groupId: Long,
)

data class GetTodayParfaitResult(
    val parfaitId: Long,
    val date: LocalDate,
    val status: ParfaitStatus,
    val lastClosedDate: LocalDate?,
    val groupMembers: List<GroupMemberResult>,
    val background: BackgroundResult?,
    val images: List<TodayParfaitImageResult>?,
)

data class GroupMemberResult(
    val id: Long,
    val nickname: String,
    val nametagChip: NameTagChipType,
)

data class BackgroundResult(
    val type: BackgroundType,
    val value: String,
)

data class TodayParfaitImageResult(
    val parfaitImageId: Long,
    val imageId: Long,
    val imageUrl: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val borderType: BorderType,
    val borderColor: String?,
    val borderWidth: Double?,
    val placedBy: PlacedByResult,
    val createdAt: LocalDateTime,
)

data class PlacedByResult(
    val groupMemberId: Long,
    val nickname: String,
    val nametagChip: NameTagChipType,
    val ownerType: OwnerType,
)
