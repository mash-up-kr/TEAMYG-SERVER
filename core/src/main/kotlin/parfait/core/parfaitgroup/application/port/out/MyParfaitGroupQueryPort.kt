package parfait.core.parfaitgroup.application.port.out

import parfait.core.parfait.domain.ToppingType
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitimage.domain.BorderType
import java.time.LocalDateTime

interface MyParfaitGroupQueryPort {
    fun findAllByMemberId(memberId: Long): List<MyParfaitGroupSummary>

    /** 이미지·텍스트를 모두 토핑으로 보고 가장 최근 토핑을 기준으로 요약한다. */
    fun findAllIncludingTextsByMemberId(memberId: Long): List<MyParfaitGroupSummary>
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
    val recentToppingType: ToppingType? = null,
)
