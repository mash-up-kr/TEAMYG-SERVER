package parfait.core.parfaitgroup.application.port.out

import parfait.core.parfaitgroup.domain.GroupNickname
import parfait.core.parfaitgroup.domain.ParfaitGroupMember

interface ParfaitGroupMemberQueryPort {
    fun findByGroupIdAndMemberId(
        groupId: Long,
        memberId: Long,
    ): ParfaitGroupMember?

    fun findAllByGroupId(groupId: Long): List<ParfaitGroupMember>

    fun findAllMembershipsByMemberId(memberId: Long): List<ParfaitGroupMember>

    fun existsByGroupIdAndMemberId(
        groupId: Long,
        memberId: Long,
    ): Boolean

    fun existsByGroupIdAndNickname(
        groupId: Long,
        nickname: GroupNickname,
    ): Boolean

    fun countByGroupId(groupId: Long): Int

    fun findAllByIds(groupMemberIds: Collection<Long>): List<ParfaitGroupMember>
}
