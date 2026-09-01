package parfait.core.notification.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.notification.domain.DevicePlatform
import parfait.core.notification.domain.DeviceToken
import parfait.core.notification.port.`in`.RegisterDeviceTokenCommand
import parfait.core.notification.port.out.DeviceTokenQueryPort
import parfait.core.notification.port.out.DeviceTokenSavePort

class RegisterDeviceTokenServiceTest {
    private val deviceTokenQueryPort = mockk<DeviceTokenQueryPort>()
    private val deviceTokenSavePort = mockk<DeviceTokenSavePort>(relaxed = true)
    private val service = RegisterDeviceTokenService(deviceTokenQueryPort, deviceTokenSavePort)

    @Test
    fun `등록된 적 없는 토큰이면 새 DeviceToken을 저장한다`() {
        every { deviceTokenQueryPort.findByToken("tok-1") } returns null
        val saved = slot<DeviceToken>()

        service.register(
            RegisterDeviceTokenCommand(
                memberId = 42L,
                sessionId = "s1",
                token = "tok-1",
                platform = DevicePlatform.IOS,
            ),
        )

        verify { deviceTokenQueryPort.findByToken("tok-1") }
        verify { deviceTokenSavePort.save(capture(saved)) }
        saved.captured.id shouldBe null
        saved.captured.token shouldBe "tok-1"
        saved.captured.memberId shouldBe 42L
        saved.captured.sessionId shouldBe "s1"
        saved.captured.platform shouldBe DevicePlatform.IOS
    }

    @Test
    fun `이미 있는 토큰이면 소유자, 세션, 플랫폼을 갱신해 저장한다`() {
        val existing =
            DeviceToken(
                token = "tok-1",
                memberId = 1L,
                platform = DevicePlatform.IOS,
                sessionId = "old-session",
                id = 10L,
            )
        every { deviceTokenQueryPort.findByToken("tok-1") } returns existing
        val saved = slot<DeviceToken>()

        service.register(
            RegisterDeviceTokenCommand(
                memberId = 2L,
                sessionId = "new-session",
                token = "tok-1",
                platform = DevicePlatform.ANDROID,
            ),
        )

        verify { deviceTokenQueryPort.findByToken("tok-1") }
        verify { deviceTokenSavePort.save(capture(saved)) }
        saved.captured.id shouldBe 10L
        saved.captured.memberId shouldBe 2L
        saved.captured.sessionId shouldBe "new-session"
        saved.captured.platform shouldBe DevicePlatform.ANDROID
    }

    @Test
    fun `sessionId가 null이어도 등록된다`() {
        every { deviceTokenQueryPort.findByToken("tok-1") } returns null
        val saved = slot<DeviceToken>()

        service.register(
            RegisterDeviceTokenCommand(
                memberId = 42L,
                sessionId = null,
                token = "tok-1",
                platform = DevicePlatform.ANDROID,
            ),
        )

        verify { deviceTokenQueryPort.findByToken("tok-1") }
        verify { deviceTokenSavePort.save(capture(saved)) }
        saved.captured.sessionId shouldBe null
    }
}
