package parfait.external.kakao

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.RemoteKeySourceException
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.out.KakaoIdTokenVerifyPort
import parfait.core.exception.BusinessException
import java.io.IOException
import java.net.URL
import java.text.ParseException
import java.util.Date

@Component
class KakaoIdTokenVerifyAdapter(
    @Value("\${kakao.issuer}") private val issuer: String,
    @Value("\${kakao.app-key}") private val appKey: String,
    @Value("\${kakao.jwks-uri}") jwksUri: String,
) : KakaoIdTokenVerifyPort {
    private val log = LoggerFactory.getLogger(KakaoIdTokenVerifyAdapter::class.java)

    private val jwtProcessor: ConfigurableJWTProcessor<SecurityContext> =
        DefaultJWTProcessor<SecurityContext>().apply {
            val keySource: JWKSource<SecurityContext> = RemoteJWKSet(URL(jwksUri))
            jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, keySource)
        }

    override fun verify(
        idToken: String,
        nonce: String,
    ): String {
        val claims = parseAndVerifySignature(idToken)
        return validateClaims(claims, nonce)
    }

    private fun parseAndVerifySignature(idToken: String): JWTClaimsSet =
        try {
            jwtProcessor.process(idToken, null)
        } catch (e: RemoteKeySourceException) {
            when (e.cause) {
                is IOException -> {
                    log.warn("카카오 JWKS 서버 연결 실패", e)
                    throw BusinessException(AuthErrorCode.KAKAO_SERVER_UNAVAILABLE)
                }
                else -> {
                    log.warn("카카오 JWKS 응답이 비정상입니다", e)
                    throw BusinessException(AuthErrorCode.KAKAO_JWKS_FETCH_FAILED)
                }
            }
        } catch (e: BadJOSEException) {
            log.warn("ID 토큰 서명 검증 실패", e)
            throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
        } catch (e: ParseException) {
            log.warn("ID 토큰 파싱 실패", e)
            throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
        } catch (e: JOSEException) {
            log.warn("카카오 JWKS 조회 실패", e)
            throw BusinessException(AuthErrorCode.KAKAO_JWKS_FETCH_FAILED)
        }

    private fun validateClaims(
        claims: JWTClaimsSet,
        nonce: String,
    ): String {
        val expirationTime = claims.expirationTime
        val actualNonce =
            try {
                claims.getStringClaim(CLAIM_NONCE)
            } catch (e: ParseException) {
                log.warn("ID 토큰 클레임 검증 실패: nonce 클레임 파싱 실패", e)
                throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
            }

        if (claims.issuer != issuer) {
            log.warn("ID 토큰 클레임 검증 실패: iss 불일치 (actual={}, expected={})", claims.issuer, issuer)
            throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
        }
        if (!claims.audience.contains(appKey)) {
            log.warn("ID 토큰 클레임 검증 실패: aud 불일치 (actual={}, expected={})", claims.audience, appKey)
            throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
        }
        if (actualNonce != nonce) {
            log.warn("ID 토큰 클레임 검증 실패: nonce 불일치 (actual={}, expected={})", actualNonce, nonce)
            throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
        }
        if (expirationTime == null || !expirationTime.after(Date())) {
            log.warn("ID 토큰 클레임 검증 실패: exp 만료 또는 없음 (exp={})", expirationTime)
            throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
        }
        val subject = claims.subject
        if (subject == null) {
            log.warn("ID 토큰 클레임 검증 실패: sub 없음")
            throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
        }
        return subject
    }

    companion object {
        private const val CLAIM_NONCE = "nonce"
    }
}
