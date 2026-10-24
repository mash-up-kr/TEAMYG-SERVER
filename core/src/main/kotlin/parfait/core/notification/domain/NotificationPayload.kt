package parfait.core.notification.domain

import java.time.LocalDate

sealed interface NotificationPayload {
    val groupId: Long
    val parfaitId: Long
    val parfaitDate: LocalDate
    val actorMemberId: Long
}
