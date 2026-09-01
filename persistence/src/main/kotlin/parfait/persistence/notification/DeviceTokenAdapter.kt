package parfait.persistence.notification

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.notification.domain.DeviceToken
import parfait.core.notification.port.out.DeviceTokenDeletePort
import parfait.core.notification.port.out.DeviceTokenQueryPort
import parfait.core.notification.port.out.DeviceTokenSavePort
import parfait.persistence.repository.DeviceTokenRepository
import parfait.core.notification.domain.DevicePlatform as CoreDevicePlatform
import parfait.persistence.entity.DevicePlatform as DevicePlatformEntity
import parfait.persistence.entity.DeviceToken as DeviceTokenEntity

@Component
@Transactional
class DeviceTokenAdapter(
    private val deviceTokenRepository: DeviceTokenRepository,
) : DeviceTokenQueryPort,
    DeviceTokenSavePort,
    DeviceTokenDeletePort {
    @Transactional(readOnly = true)
    override fun findByToken(token: String): DeviceToken? = deviceTokenRepository.findByToken(token)?.toDomain()

    override fun save(deviceToken: DeviceToken): DeviceToken =
        // 같은 신규 token으로 두 요청이 동시에 insert하는 극히 드문 경합은 두 번째 요청에
        // DataIntegrityViolationException으로 표면화된다. POST /devices는 앱 시작·onNewToken·권한 허용
        // 시마다 재호출되므로, 이후 재시도에서 findByToken이 커밋된 행을 찾아 update 경로로 수렴한다.
        deviceTokenRepository.save(deviceToken.toEntity()).toDomain()

    override fun delete(
        memberId: Long,
        sessionId: String,
    ) {
        deviceTokenRepository.deleteByMemberIdAndSessionId(memberId, sessionId)
    }

    override fun deleteAllByMemberId(memberId: Long) {
        deviceTokenRepository.deleteByMemberId(memberId)
    }

    private fun DeviceTokenEntity.toDomain(): DeviceToken =
        DeviceToken(
            token = token,
            memberId = memberId,
            platform = platform.toCore(),
            sessionId = sessionId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            id = id,
        )

    private fun DeviceToken.toEntity(): DeviceTokenEntity =
        DeviceTokenEntity(
            memberId = memberId,
            token = token,
            platform = platform.toEntity(),
            sessionId = sessionId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            id = id,
        )

    private fun CoreDevicePlatform.toEntity(): DevicePlatformEntity =
        when (this) {
            CoreDevicePlatform.IOS -> DevicePlatformEntity.IOS
            CoreDevicePlatform.ANDROID -> DevicePlatformEntity.ANDROID
        }

    private fun DevicePlatformEntity.toCore(): CoreDevicePlatform =
        when (this) {
            DevicePlatformEntity.IOS -> CoreDevicePlatform.IOS
            DevicePlatformEntity.ANDROID -> CoreDevicePlatform.ANDROID
        }
}
