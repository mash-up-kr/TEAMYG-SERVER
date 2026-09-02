package parfait.core.notification.event

/** place() 커밋 후 OutboxPollingWorker 를 즉시 깨우기 위한 신호. 페이로드는 toppingId 뿐. */
data class ToppingPlacedEvent(
    val toppingId: Long,
)
