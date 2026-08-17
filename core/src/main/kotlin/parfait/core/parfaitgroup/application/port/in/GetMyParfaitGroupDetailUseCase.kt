@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

import parfait.core.parfaitgroup.domain.NameTagChipType

interface GetMyParfaitGroupDetailUseCase {
    fun get(
        memberId: Long,
        groupId: Long,
    ): MyParfaitGroupDetailResult
}

data class MyParfaitGroupDetailResult(
    val groupId: Long,
    val groupName: String,
    val groupNickname: String,
    val inviteCode: String,
    val memberLimit: Int,
    val members: List<ParfaitGroupMemberResult>,
)

data class ParfaitGroupMemberResult(
    val memberId: Long,
    val groupNickname: String,
    val nametagChip: NameTagChipType?,
)
