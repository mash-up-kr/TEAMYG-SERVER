package parfait.core.parfaitgroup.application.port.out

import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitimage.domain.BorderType
import java.time.LocalDateTime

interface MyParfaitGroupQueryPort {
    fun findAllByMemberId(memberId: Long): List<MyParfaitGroupSummary>
}

data class MyParfaitGroupSummary(
    val groupId: Long,
    val groupName: String,
    val recentImageUrl: String?,
    val recentImageBorderType: BorderType?,
    val recentImageBorderColor: String?,
    val recentImageBorderWidth: Double?,
    val recentImageUploadedAt: LocalDateTime,
    val lastPlacedByNametagChip: NameTagChipType,
)
