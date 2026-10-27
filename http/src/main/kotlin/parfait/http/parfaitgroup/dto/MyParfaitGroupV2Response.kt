package parfait.http.parfaitgroup.dto

import parfait.core.parfait.domain.ToppingType
import parfait.core.parfaitgroup.application.port.`in`.MyParfaitGroupResult
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitimage.domain.BorderType
import java.time.LocalDateTime

/**
 * v2 내 그룹 목록 원소. v1(`MyParfaitGroupResponse`)에 `recentToppingType`을 더한다.
 * 오늘 가장 최근 토핑이 텍스트면 `recentImageUrl`과 테두리 필드는 null이다.
 */
data class MyParfaitGroupV2Response(
    val groupId: Long,
    val groupName: String,
    val recentToppingType: ToppingType?,
    val recentImageUrl: String?,
    val recentImageBorderType: BorderType?,
    val recentImageBorderColor: String?,
    val recentImageBorderWidth: Double?,
    val recentImageUploadedAt: LocalDateTime,
    val lastPlacedByNameTagChip: NameTagChipType,
) {
    companion object {
        fun from(result: MyParfaitGroupResult): MyParfaitGroupV2Response =
            MyParfaitGroupV2Response(
                groupId = result.groupId,
                groupName = result.groupName,
                recentToppingType = result.recentToppingType,
                recentImageUrl = result.recentImageUrl,
                recentImageBorderType = result.recentImageBorderType,
                recentImageBorderColor = result.recentImageBorderColor,
                recentImageBorderWidth = result.recentImageBorderWidth,
                recentImageUploadedAt = result.recentImageUploadedAt,
                lastPlacedByNameTagChip = result.lastPlacedByNametagChip,
            )
    }
}
