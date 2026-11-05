package parfait.core.notification.domain

import java.time.LocalDate

/**
 * P-06 예약 행 payload. NotificationPayload 의 공통 필드를 채우기 위해
 * parfaitId·actorMemberId 는 윈도우를 연 첫 활동의 값을 쓴다(문구에는 쓰지 않는다).
 */
data class ActivityDigestPayload(
    override val groupId: Long,
    override val parfaitId: Long,
    override val parfaitDate: LocalDate,
    override val actorMemberId: Long,
) : NotificationPayload
