package parfait.core.auth.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.`in`.ReissueResult
import parfait.core.auth.port.`in`.ReissueUseCase
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.auth.port.out.TokenQueryPort
import parfait.core.auth.port.out.TokenSavePort
import parfait.core.auth.port.out.TokenValidatePort
import parfait.core.exception.BusinessException
import parfait.core.member.port.out.MemberQueryPort

@Service
class ReissueService(
    private val tokenValidatePort: TokenValidatePort,
    private val tokenQueryPort: TokenQueryPort,
    private val tokenIssuePort: TokenIssuePort,
    private val tokenSavePort: TokenSavePort,
    private val memberQueryPort: MemberQueryPort,
    @Value("\${jwt.access-token-expiration-seconds}") private val accessTokenExpiresInSeconds: Long,
    @Value("\${jwt.refresh-token-expiration-seconds}") private val refreshTokenTtlSeconds: Long,
) : ReissueUseCase {
    override fun reissue(refreshToken: String): ReissueResult {
        val claims = tokenValidatePort.validateRefreshToken(refreshToken)
        val storedRefreshToken = tokenQueryPort.findRefreshToken(claims.memberId, claims.sessionId)
        if (storedRefreshToken == null || storedRefreshToken != refreshToken) {
            throw BusinessException(AuthErrorCode.INVALID_TOKEN)
        }
        if (!memberQueryPort.existsById(claims.memberId)) {
            throw BusinessException(AuthErrorCode.MEMBER_NOT_FOUND)
        }

        val newAccessToken = tokenIssuePort.createAccessToken(claims.memberId, claims.sessionId)
        val newRefreshToken = tokenIssuePort.createRefreshToken(claims.memberId, claims.sessionId)
        tokenSavePort.save(claims.memberId, claims.sessionId, newRefreshToken, refreshTokenTtlSeconds)

        return ReissueResult(newAccessToken, newRefreshToken, accessTokenExpiresInSeconds)
    }
}
