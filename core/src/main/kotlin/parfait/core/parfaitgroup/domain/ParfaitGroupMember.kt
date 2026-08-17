package parfait.core.parfaitgroup.domain

import java.time.LocalDateTime

class ParfaitGroupMember private constructor(
    val id: Long?,
    val parfaitGroupId: Long,
    val memberId: Long,
    val groupNickname: GroupNickname,
    val nametagChip: NameTagChipType?,
    val joinedAt: LocalDateTime,
    val leftAt: LocalDateTime?,
) {
    fun changeNickname(groupNickname: String): ParfaitGroupMember =
        ParfaitGroupMember(
            id = id,
            parfaitGroupId = parfaitGroupId,
            memberId = memberId,
            groupNickname = GroupNickname.of(groupNickname),
            nametagChip = nametagChip,
            joinedAt = joinedAt,
            leftAt = leftAt,
        )

    fun leave(leftAt: LocalDateTime = LocalDateTime.now()): ParfaitGroupMember =
        ParfaitGroupMember(
            id = id,
            parfaitGroupId = parfaitGroupId,
            memberId = memberId,
            groupNickname = GroupNickname.unknown(),
            nametagChip = NameTagChipType.RELEASED,
            joinedAt = joinedAt,
            leftAt = leftAt,
        )

    companion object {
        fun join(
            parfaitGroupId: Long,
            memberId: Long,
            groupNickname: String,
            nametagChip: NameTagChipType,
            joinedAt: LocalDateTime = LocalDateTime.now(),
        ): ParfaitGroupMember =
            ParfaitGroupMember(
                id = null,
                parfaitGroupId = parfaitGroupId,
                memberId = memberId,
                groupNickname = GroupNickname.of(groupNickname),
                nametagChip = nametagChip,
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
            // 마지막 파라미터 + 기본값 null로 둔다: parfaitimage/member 테스트의 기존 호출부들이
            // (id, parfaitGroupId, memberId, groupNickname, joinedAt) 5개만 위치 인자로 넘기므로,
            // 순서를 바꾸거나 앞으로 옮기면 그 호출부들이 전부 깨진다.
            nametagChip: NameTagChipType? = null,
        ): ParfaitGroupMember =
            ParfaitGroupMember(
                id = id,
                parfaitGroupId = parfaitGroupId,
                memberId = memberId,
                groupNickname = GroupNickname.of(groupNickname),
                nametagChip = nametagChip,
                joinedAt = joinedAt,
                leftAt = leftAt,
            )
    }
}
