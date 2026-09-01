package parfait.core.auth.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.port.`in`.KakaoLoginResult
import parfait.core.auth.port.`in`.KakaoLoginUseCase
import parfait.core.auth.port.out.KakaoIdTokenVerifyPort
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.auth.port.out.TokenSavePort
import parfait.core.member.port.out.MemberQueryPort
import java.util.UUID

@Service
class KakaoLoginService(
    private val kakaoIdTokenVerifyPort: KakaoIdTokenVerifyPort,
    private val memberQueryPort: MemberQueryPort,
    private val tokenIssuePort: TokenIssuePort,
    private val tokenSavePort: TokenSavePort,
    @Value("\${jwt.access-token-expiration-seconds}") private val accessTokenExpiresInSeconds: Long,
    @Value("\${jwt.refresh-token-expiration-seconds}") private val refreshTokenTtlSeconds: Long,
) : KakaoLoginUseCase {
    override fun login(
        idToken: String,
        nonce: String,
    ): KakaoLoginResult {
        val providerUserId = kakaoIdTokenVerifyPort.verify(idToken, nonce)
        val memberId = memberQueryPort.findMemberIdByProvider(LoginProvider.KAKAO, providerUserId)

        return if (memberId == null) {
            val registrationToken = tokenIssuePort.createRegistrationToken(LoginProvider.KAKAO, providerUserId)
            KakaoLoginResult.NewUser(registrationToken)
        } else {
            val sessionId = UUID.randomUUID().toString()
            val accessToken = tokenIssuePort.createAccessToken(memberId, sessionId)
            val refreshToken = tokenIssuePort.createRefreshToken(memberId, sessionId)
            tokenSavePort.save(memberId, sessionId, refreshToken, refreshTokenTtlSeconds)
            KakaoLoginResult.ExistingMember(accessToken, refreshToken, accessTokenExpiresInSeconds)
        }
    }
}
