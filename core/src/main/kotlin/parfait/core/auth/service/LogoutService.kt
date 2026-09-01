package parfait.core.auth.service

import org.springframework.stereotype.Service
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.`in`.LogoutUseCase
import parfait.core.auth.port.out.TokenDeletePort
import parfait.core.auth.port.out.TokenValidatePort
import parfait.core.exception.BusinessException
import parfait.core.notification.port.out.DeviceTokenDeletePort

@Service
class LogoutService(
    private val tokenValidatePort: TokenValidatePort,
    private val tokenDeletePort: TokenDeletePort,
    private val deviceTokenDeletePort: DeviceTokenDeletePort,
) : LogoutUseCase {
    override fun logout(
        memberId: Long,
        refreshToken: String,
    ) {
        val claims = tokenValidatePort.validateRefreshToken(refreshToken)
        if (claims.memberId != memberId) {
            throw BusinessException(AuthErrorCode.FORBIDDEN_REFRESH_TOKEN)
        }
        tokenDeletePort.delete(claims.memberId, claims.sessionId)
        deviceTokenDeletePort.delete(claims.memberId, claims.sessionId)
    }
}
