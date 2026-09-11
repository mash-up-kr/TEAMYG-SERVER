@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitimage.domain.BorderType
import java.time.LocalDateTime

interface GetMyParfaitGroupsUseCase {
    fun getAll(memberId: Long): List<MyParfaitGroupResult>
}

data class MyParfaitGroupResult(
    val groupId: Long,
    val groupName: String,
    val recentImageUrl: String?,
    val recentImageBorderType: BorderType?,
    val recentImageBorderColor: String?,
    val recentImageBorderWidth: Double?,
    val recentImageUploadedAt: LocalDateTime,
    val lastPlacedByNametagChip: NameTagChipType,
)
