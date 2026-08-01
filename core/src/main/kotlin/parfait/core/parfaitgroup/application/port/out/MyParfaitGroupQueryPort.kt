package parfait.core.parfaitgroup.application.port.out

import java.time.LocalDateTime

interface MyParfaitGroupQueryPort {
    fun findAllByMemberId(memberId: Long): List<MyParfaitGroupSummary>
}

data class MyParfaitGroupSummary(
    val groupId: Long,
    val groupName: String,
    val recentImageUrl: String?,
    val recentImageUploadedAt: LocalDateTime?,
)
