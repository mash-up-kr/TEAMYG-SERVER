@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

import parfait.core.parfaitgroup.domain.NameTagChipType
import java.time.LocalDateTime

interface GetMyParfaitGroupsUseCase {
    fun getAll(memberId: Long): List<MyParfaitGroupResult>
}

data class MyParfaitGroupResult(
    val groupId: Long,
    val groupName: String,
    val recentImageUrl: String?,
    val recentImageUploadedAt: LocalDateTime?,
    val lastPlacedByNametagChip: NameTagChipType?,
)
