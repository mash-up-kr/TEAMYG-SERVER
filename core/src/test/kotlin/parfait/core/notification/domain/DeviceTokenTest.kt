package parfait.core.notification.domain

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DeviceTokenTest {
    @Test
    fun `register는 id 없는 새 토큰을 만들고 필드를 채운다`() {
        val deviceToken =
            DeviceToken.register(
                memberId = 42L,
                sessionId = "session-1",
                token = "fcm-token",
                platform = DevicePlatform.IOS,
            )

        deviceToken.id.shouldBeNull()
        deviceToken.token shouldBe "fcm-token"
        deviceToken.memberId shouldBe 42L
        deviceToken.sessionId shouldBe "session-1"
        deviceToken.platform shouldBe DevicePlatform.IOS
    }

    @Test
    fun `register는 sessionId가 null이어도 만들 수 있다`() {
        val deviceToken =
            DeviceToken.register(
                memberId = 42L,
                sessionId = null,
                token = "fcm-token",
                platform = DevicePlatform.ANDROID,
            )

        deviceToken.sessionId.shouldBeNull()
    }

    @Test
    fun `reassign은 소유자, 세션, 플랫폼을 갱신하고 updatedAt을 밀어올린다`() {
        val deviceToken =
            DeviceToken.register(
                memberId = 1L,
                sessionId = "old-session",
                token = "fcm-token",
                platform = DevicePlatform.IOS,
            )
        val originalUpdatedAt = deviceToken.updatedAt
        val originalCreatedAt = deviceToken.createdAt

        deviceToken.reassign(memberId = 2L, sessionId = "new-session", platform = DevicePlatform.ANDROID)

        deviceToken.memberId shouldBe 2L
        deviceToken.sessionId shouldBe "new-session"
        deviceToken.platform shouldBe DevicePlatform.ANDROID
        (deviceToken.updatedAt >= originalUpdatedAt) shouldBe true
        deviceToken.createdAt shouldBe originalCreatedAt
    }
}
