package parfait.core.parfaitgroup.application.port.out

import parfait.core.parfaitgroup.domain.NameTagChipType
import java.time.LocalDateTime

interface MyParfaitGroupQueryPort {
    fun findAllByMemberId(memberId: Long): List<MyParfaitGroupSummary>
}

data class MyParfaitGroupSummary(
    val groupId: Long,
    val groupName: String,
    val recentImageUrl: String?,
    val recentImageUploadedAt: LocalDateTime?,
    val lastPlacedByNametagChip: NameTagChipType?,
)
