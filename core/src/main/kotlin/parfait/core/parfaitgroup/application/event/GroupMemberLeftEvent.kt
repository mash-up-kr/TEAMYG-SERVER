package parfait.core.parfaitgroup.application.event

/** 멤버가 그룹을 나간 트랜잭션이 커밋되면 Redis 의 group:member 키를 지우기 위한 신호. */
data class GroupMemberLeftEvent(
    val groupId: Long,
    val memberId: Long,
)
