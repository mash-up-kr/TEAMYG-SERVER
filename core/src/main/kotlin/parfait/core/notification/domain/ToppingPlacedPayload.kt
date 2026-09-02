package parfait.core.notification.domain

import java.time.LocalDate

/** notification_outbox.payload 로 JSON 직렬화되는 식별자 묶음. 렌더된 문구는 담지 않는다. */
data class ToppingPlacedPayload(
    val groupId: Long,
    val parfaitId: Long,
    val parfaitDate: LocalDate,
    val actorMemberId: Long,
)
