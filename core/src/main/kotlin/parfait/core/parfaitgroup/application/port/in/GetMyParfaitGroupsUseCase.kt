@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

import parfait.core.parfait.domain.ToppingType
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitimage.domain.BorderType
import java.time.LocalDateTime

interface GetMyParfaitGroupsUseCase {
    fun getAll(memberId: Long): List<MyParfaitGroupResult>

    /** 이미지뿐 아니라 텍스트까지 포함한 가장 최근 토핑을 기준으로 조회한다 (v2). */
    fun getAllIncludingTexts(memberId: Long): List<MyParfaitGroupResult>
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
    val recentToppingType: ToppingType? = null,
)
