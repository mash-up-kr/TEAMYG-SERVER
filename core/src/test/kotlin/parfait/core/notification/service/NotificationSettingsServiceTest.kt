package parfait.core.notification.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.notification.domain.NotificationSettings
import parfait.core.notification.port.`in`.UpdateNotificationSettingsCommand
import parfait.core.notification.port.out.NotificationSettingsQueryPort
import parfait.core.notification.port.out.NotificationSettingsUpdatePort

class NotificationSettingsServiceTest {
    private val queryPort = mockk<NotificationSettingsQueryPort>()
    private val updatePort = mockk<NotificationSettingsUpdatePort>(relaxed = true)
    private val service = NotificationSettingsService(queryPort, updatePort)

    @Test
    fun `조회는 저장된 설정을 그대로 돌려준다`() {
        every { queryPort.findByMemberId(42L) } returns
            NotificationSettings(dailyReminderEnabled = false, activityDigestEnabled = true)

        service.get(42L) shouldBe NotificationSettings(dailyReminderEnabled = false, activityDigestEnabled = true)
    }

    @Test
    fun `변경은 요청에 담긴 값만 저장하고 다시 읽은 최종 설정을 돌려준다`() {
        every { queryPort.findByMemberId(42L) } returns
            NotificationSettings(dailyReminderEnabled = true, activityDigestEnabled = true)

        val result =
            service.update(
                UpdateNotificationSettingsCommand(
                    memberId = 42L,
                    dailyReminderEnabled = null,
                    activityDigestEnabled = true,
                ),
            )

        result shouldBe NotificationSettings(dailyReminderEnabled = true, activityDigestEnabled = true)
        verify { updatePort.update(42L, null, true) }
    }

    @Test
    fun `변경할 값이 하나도 없으면 저장하지 않는다`() {
        every { queryPort.findByMemberId(42L) } returns NotificationSettings.DEFAULT

        service.update(UpdateNotificationSettingsCommand(42L, null, null)) shouldBe NotificationSettings.DEFAULT

        verify(exactly = 0) { updatePort.update(any(), any(), any()) }
    }
}
