package parfait.core.parfaitgroup.application.event

/** 그룹 version 이 오른 트랜잭션이 커밋되면 Redis 의 group:version 키를 지우기 위한 신호. */
data class GroupVersionBumpedEvent(
    val groupId: Long,
)
