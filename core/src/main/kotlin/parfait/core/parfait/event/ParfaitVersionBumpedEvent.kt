package parfait.core.parfait.event

/** 캔버스 version 이 오른 트랜잭션이 커밋되면 Redis 의 parfait:version 키를 지우기 위한 신호. */
data class ParfaitVersionBumpedEvent(
    val parfaitId: Long,
)
