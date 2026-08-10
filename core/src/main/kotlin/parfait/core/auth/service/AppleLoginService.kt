package parfait.core.auth.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.port.`in`.AppleLoginResult
import parfait.core.auth.port.`in`.AppleLoginUseCase
import parfait.core.auth.port.out.AppleAuthorizationCodeExchangePort
import parfait.core.auth.port.out.AppleIdTokenVerifyPort
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.auth.port.out.TokenSavePort
import parfait.core.member.port.out.MemberAppleRefreshTokenSavePort
import parfait.core.member.port.out.MemberQueryPort
import java.util.UUID

@Service
class AppleLoginService(
    private val appleIdTokenVerifyPort: AppleIdTokenVerifyPort,
    private val appleAuthorizationCodeExchangePort: AppleAuthorizationCodeExchangePort,
    private val memberQueryPort: MemberQueryPort,
    private val memberAppleRefreshTokenSavePort: MemberAppleRefreshTokenSavePort,
    private val tokenIssuePort: TokenIssuePort,
    private val tokenSavePort: TokenSavePort,
    @Value("\${jwt.access-token-expiration-seconds}") private val accessTokenExpiresInSeconds: Long,
    @Value("\${jwt.refresh-token-expiration-seconds}") private val refreshTokenTtlSeconds: Long,
) : AppleLoginUseCase {
    override fun login(
        identityToken: String,
        nonce: String,
        authorizationCode: String,
    ): AppleLoginResult {
        val providerUserId = appleIdTokenVerifyPort.verify(identityToken, nonce)
        val appleRefreshToken = appleAuthorizationCodeExchangePort.exchange(authorizationCode)
        val memberId = memberQueryPort.findMemberIdByProvider(LoginProvider.APPLE, providerUserId)

        return if (memberId == null) {
            val registrationToken =
                tokenIssuePort.createRegistrationToken(LoginProvider.APPLE, providerUserId, appleRefreshToken)
            AppleLoginResult.NewUser(registrationToken)
        } else {
            memberAppleRefreshTokenSavePort.saveRefreshToken(memberId, appleRefreshToken)
            val sessionId = UUID.randomUUID().toString()
            val accessToken = tokenIssuePort.createAccessToken(memberId)
            val refreshToken = tokenIssuePort.createRefreshToken(memberId, sessionId)
            tokenSavePort.save(memberId, sessionId, refreshToken, refreshTokenTtlSeconds)
            AppleLoginResult.ExistingMember(accessToken, refreshToken, accessTokenExpiresInSeconds)
        }
    }
}
