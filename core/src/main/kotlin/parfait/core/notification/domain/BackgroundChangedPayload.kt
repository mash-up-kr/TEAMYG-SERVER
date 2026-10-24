package parfait.core.notification.domain

import java.time.LocalDate

data class BackgroundChangedPayload(
    override val groupId: Long,
    override val parfaitId: Long,
    override val parfaitDate: LocalDate,
    override val actorMemberId: Long,
) : NotificationPayload
