package parfait.core.notification.event

/** 배경 변경 커밋 후 OutboxPollingWorker 를 즉시 깨우기 위한 신호. 페이로드는 parfaitId 뿐. */
data class BackgroundChangedEvent(
    val parfaitId: Long,
)
