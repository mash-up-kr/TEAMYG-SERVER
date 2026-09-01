package parfait.core.notification.domain

import java.time.LocalDateTime

class DeviceToken(
    val token: String,
    var memberId: Long,
    var platform: DevicePlatform,
    var sessionId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    val id: Long? = null,
) {
    fun reassign(
        memberId: Long,
        sessionId: String?,
        platform: DevicePlatform,
    ) {
        this.memberId = memberId
        this.sessionId = sessionId
        this.platform = platform
        this.updatedAt = LocalDateTime.now()
    }

    companion object {
        fun register(
            memberId: Long,
            sessionId: String?,
            token: String,
            platform: DevicePlatform,
        ): DeviceToken =
            DeviceToken(
                token = token,
                memberId = memberId,
                platform = platform,
                sessionId = sessionId,
            )
    }
}
