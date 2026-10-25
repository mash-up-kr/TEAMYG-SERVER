package parfait.core.notification.domain

import java.time.LocalDate

data class TextPlacedPayload(
    override val groupId: Long,
    override val parfaitId: Long,
    override val parfaitDate: LocalDate,
    override val actorMemberId: Long,
    val text: String,
) : NotificationPayload
