package parfait.persistence.notification

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.notification.domain.NotificationSettings
import parfait.core.notification.port.out.NotificationSettingsQueryPort
import parfait.core.notification.port.out.NotificationSettingsUpdatePort
import parfait.persistence.repository.MemberRepository

@Component
class NotificationSettingsAdapter(
    private val memberRepository: MemberRepository,
) : NotificationSettingsQueryPort,
    NotificationSettingsUpdatePort {
    @Transactional(readOnly = true)
    override fun findByMemberId(memberId: Long): NotificationSettings =
        memberRepository.findById(memberId).orElse(null)?.let {
            NotificationSettings(
                dailyReminderEnabled = it.dailyReminderEnabled,
                activityDigestEnabled = it.activityDigestEnabled,
            )
        } ?: NotificationSettings.DEFAULT

    @Transactional
    override fun update(
        memberId: Long,
        dailyReminderEnabled: Boolean?,
        activityDigestEnabled: Boolean?,
    ) {
        memberRepository.updateNotificationSettings(memberId, dailyReminderEnabled, activityDigestEnabled)
    }
}
