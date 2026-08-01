package parfait.core.parfaitgroup.domain

import java.time.LocalDateTime

class ParfaitGroup private constructor(
    val id: Long?,
    val name: GroupName,
    val inviteCode: InviteCode,
    val memberLimit: GroupMemberLimit,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun requireId(): Long = requireNotNull(id) { "저장된 그룹의 id가 필요합니다" }

    fun validateJoin(
        alreadyJoined: Boolean,
        currentMemberCount: Int,
    ) {
        if (alreadyJoined) {
            throw ParfaitGroupException(ParfaitGroupError.GROUP_ALREADY_JOINED)
        }
        if (currentMemberCount >= memberLimit.value) {
            throw ParfaitGroupException(ParfaitGroupError.GROUP_MEMBER_LIMIT_REACHED)
        }
    }

    companion object {
        fun create(
            name: String,
            inviteCode: InviteCode,
            memberLimit: Int,
            now: LocalDateTime = LocalDateTime.now(),
        ): ParfaitGroup =
            ParfaitGroup(
                id = null,
                name = GroupName.of(name),
                inviteCode = inviteCode,
                memberLimit = GroupMemberLimit.of(memberLimit),
                createdAt = now,
                updatedAt = now,
            )

        fun reconstitute(
            id: Long,
            name: String,
            inviteCode: String,
            memberLimit: Int,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
        ): ParfaitGroup =
            ParfaitGroup(
                id = id,
                name = GroupName.of(name),
                inviteCode = InviteCode.of(inviteCode),
                memberLimit = GroupMemberLimit.of(memberLimit),
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }
}
