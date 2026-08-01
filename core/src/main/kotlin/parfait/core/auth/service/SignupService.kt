package parfait.core.auth.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.`in`.SignupResult
import parfait.core.auth.port.`in`.SignupUseCase
import parfait.core.auth.port.`in`.TermsAgreement
import parfait.core.auth.port.out.CurrentTerms
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.auth.port.out.TokenSavePort
import parfait.core.auth.port.out.TokenValidatePort
import parfait.core.auth.port.out.TosQueryPort
import parfait.core.exception.BusinessException
import parfait.core.member.domain.RandomNicknameGenerator
import parfait.core.member.port.out.MemberQueryPort
import java.util.UUID

@Service
class SignupService(
    private val tokenValidatePort: TokenValidatePort,
    private val memberQueryPort: MemberQueryPort,
    private val tosQueryPort: TosQueryPort,
    private val memberRegistrar: MemberRegistrar,
    private val tokenIssuePort: TokenIssuePort,
    private val tokenSavePort: TokenSavePort,
    private val nicknameGenerator: RandomNicknameGenerator,
    @Value("\${jwt.access-token-expiration-seconds}") private val accessTokenExpiresInSeconds: Long,
    @Value("\${jwt.refresh-token-expiration-seconds}") private val refreshTokenTtlSeconds: Long,
) : SignupUseCase {
    override fun signup(
        registrationToken: String,
        agreements: List<TermsAgreement>,
    ): SignupResult {
        val claims = tokenValidatePort.validateRegistrationToken(registrationToken)

        val currentTerms = tosQueryPort.findCurrentTerms()
        validateAgreements(agreements, currentTerms)

        if (memberQueryPort.findMemberIdByProvider(claims.provider, claims.providerUserId) != null) {
            throw BusinessException(AuthErrorCode.ALREADY_REGISTERED)
        }

        val nickname = nicknameGenerator.generate()
        val agreedTosIds = agreements.filter { it.agreed }.map { it.termsId }
        val memberId = memberRegistrar.register(claims.provider, claims.providerUserId, nickname, agreedTosIds)

        handleProviderSpecificRegistration(claims.provider, memberId)

        val sessionId = UUID.randomUUID().toString()
        val accessToken = tokenIssuePort.createAccessToken(memberId)
        val refreshToken = tokenIssuePort.createRefreshToken(memberId, sessionId)
        tokenSavePort.save(memberId, sessionId, refreshToken, refreshTokenTtlSeconds)

        return SignupResult(accessToken, refreshToken, accessTokenExpiresInSeconds)
    }

    private fun handleProviderSpecificRegistration(
        provider: LoginProvider,
        memberId: Long,
    ) {
        when (provider) {
            LoginProvider.KAKAO -> {
                // 카카오는 회원가입 시점에 별도로 저장할 provider 전용 데이터가 없다.
            }
            LoginProvider.APPLE -> {
                // TODO(#50): registrationToken에 담겨 온 애플 revoke용 refreshToken을 여기서 저장해야 한다.
                // 현재 RegistrationTokenClaims는 provider/providerUserId만 가지고 있어 처리할 데이터가 없다 —
                // #50에서 애플 로그인을 구현하며 클레임 구조를 확장하고 이 분기를 채운다.
            }
        }
    }

    private fun validateAgreements(
        agreements: List<TermsAgreement>,
        currentTerms: List<CurrentTerms>,
    ) {
        val requestedTermsIds = agreements.map { it.termsId }
        if (requestedTermsIds.size != requestedTermsIds.toSet().size) {
            throw BusinessException(AuthErrorCode.DUPLICATE_TERMS_ID)
        }

        val currentTermsIds = currentTerms.map { it.id }.toSet()
        if (!currentTermsIds.containsAll(requestedTermsIds)) {
            throw BusinessException(AuthErrorCode.TERMS_NOT_FOUND)
        }

        val agreedTermsIds = agreements.filter { it.agreed }.map { it.termsId }.toSet()
        val requiredTermsIds = currentTerms.filter { it.required }.map { it.id }
        if (!agreedTermsIds.containsAll(requiredTermsIds)) {
            throw BusinessException(AuthErrorCode.REQUIRED_TERMS_NOT_AGREED)
        }
    }
}
