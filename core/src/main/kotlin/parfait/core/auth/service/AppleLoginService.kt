package parfait.core.auth.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.port.`in`.AppleLoginResult
import parfait.core.auth.port.`in`.AppleLoginUseCase
import parfait.core.auth.port.out.AppleIdTokenVerifyPort
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.auth.port.out.TokenSavePort
import parfait.core.member.port.out.MemberQueryPort
import java.util.UUID

@Service
class AppleLoginService(
    private val appleIdTokenVerifyPort: AppleIdTokenVerifyPort,
    private val memberQueryPort: MemberQueryPort,
    private val tokenIssuePort: TokenIssuePort,
    private val tokenSavePort: TokenSavePort,
    @Value("\${jwt.access-token-expiration-seconds}") private val accessTokenExpiresInSeconds: Long,
    @Value("\${jwt.refresh-token-expiration-seconds}") private val refreshTokenTtlSeconds: Long,
) : AppleLoginUseCase {
    override fun login(
        identityToken: String,
        nonce: String,
    ): AppleLoginResult {
        val providerUserId = appleIdTokenVerifyPort.verify(identityToken, nonce)
        val memberId = memberQueryPort.findMemberIdByProvider(LoginProvider.APPLE, providerUserId)

        return if (memberId == null) {
            val registrationToken =
                tokenIssuePort.createRegistrationToken(LoginProvider.APPLE, providerUserId)
            AppleLoginResult.NewUser(registrationToken)
        } else {
            val sessionId = UUID.randomUUID().toString()
            val accessToken = tokenIssuePort.createAccessToken(memberId, sessionId)
            val refreshToken = tokenIssuePort.createRefreshToken(memberId, sessionId)
            tokenSavePort.save(memberId, sessionId, refreshToken, refreshTokenTtlSeconds)
            AppleLoginResult.ExistingMember(accessToken, refreshToken, accessTokenExpiresInSeconds)
        }
    }
}
