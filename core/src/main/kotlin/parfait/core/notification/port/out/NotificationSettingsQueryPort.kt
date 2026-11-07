package parfait.core.notification.port.out

import parfait.core.notification.domain.NotificationSettings

interface NotificationSettingsQueryPort {
    /** 회원이 없으면 기본값을 돌려준다. */
    fun findByMemberId(memberId: Long): NotificationSettings
}
