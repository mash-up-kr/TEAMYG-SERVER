package parfait.core.parfaitgroup.domain

import java.time.LocalDateTime

class ParfaitGroupMember private constructor(
    val id: Long?,
    val parfaitGroupId: Long,
    val memberId: Long,
    val groupNickname: GroupNickname,
    val joinedAt: LocalDateTime,
) {
    fun changeNickname(groupNickname: String): ParfaitGroupMember =
        ParfaitGroupMember(
            id = id,
            parfaitGroupId = parfaitGroupId,
            memberId = memberId,
            groupNickname = GroupNickname.of(groupNickname),
            joinedAt = joinedAt,
        )

    companion object {
        fun join(
            parfaitGroupId: Long,
            memberId: Long,
            groupNickname: String,
            joinedAt: LocalDateTime = LocalDateTime.now(),
        ): ParfaitGroupMember =
            ParfaitGroupMember(
                id = null,
                parfaitGroupId = parfaitGroupId,
                memberId = memberId,
                groupNickname = GroupNickname.of(groupNickname),
                joinedAt = joinedAt,
            )

        fun reconstitute(
            id: Long,
            parfaitGroupId: Long,
            memberId: Long,
            groupNickname: String,
            joinedAt: LocalDateTime,
        ): ParfaitGroupMember =
            ParfaitGroupMember(
                id = id,
                parfaitGroupId = parfaitGroupId,
                memberId = memberId,
                groupNickname = GroupNickname.of(groupNickname),
                joinedAt = joinedAt,
            )
    }
}
