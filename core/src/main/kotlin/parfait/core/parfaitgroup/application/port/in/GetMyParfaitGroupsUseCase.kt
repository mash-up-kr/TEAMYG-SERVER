@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

import java.time.LocalDateTime

interface GetMyParfaitGroupsUseCase {
    fun getAll(memberId: Long): List<MyParfaitGroupResult>
}

data class MyParfaitGroupResult(
    val groupId: Long,
    val groupName: String,
    val recentImageUrl: String?,
    val recentImageUploadedAt: LocalDateTime?,
)
