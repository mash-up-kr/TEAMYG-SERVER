package parfait.core.parfaitgroup.domain

import java.time.LocalDateTime

class ParfaitGroupMember private constructor(
    val id: Long?,
    val parfaitGroupId: Long,
    val memberId: Long,
    val groupNickname: GroupNickname,
    val joinedAt: LocalDateTime,
    val leftAt: LocalDateTime?,
) {
    fun changeNickname(groupNickname: String): ParfaitGroupMember =
        ParfaitGroupMember(
            id = id,
            parfaitGroupId = parfaitGroupId,
            memberId = memberId,
            groupNickname = GroupNickname.of(groupNickname),
            joinedAt = joinedAt,
            leftAt = leftAt,
        )

    fun leave(leftAt: LocalDateTime = LocalDateTime.now()): ParfaitGroupMember =
        ParfaitGroupMember(
            id = id,
            parfaitGroupId = parfaitGroupId,
            memberId = memberId,
            groupNickname = GroupNickname.unknown(),
            joinedAt = joinedAt,
            leftAt = leftAt,
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
                leftAt = null,
            )

        fun reconstitute(
            id: Long,
            parfaitGroupId: Long,
            memberId: Long,
            groupNickname: String,
            joinedAt: LocalDateTime,
            leftAt: LocalDateTime? = null,
        ): ParfaitGroupMember =
            ParfaitGroupMember(
                id = id,
                parfaitGroupId = parfaitGroupId,
                memberId = memberId,
                groupNickname = GroupNickname.of(groupNickname),
                joinedAt = joinedAt,
                leftAt = leftAt,
            )
    }
}
