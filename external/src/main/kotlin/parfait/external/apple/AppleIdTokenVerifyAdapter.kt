package parfait.external.apple

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
import parfait.core.auth.port.out.AppleIdTokenVerifyPort
import parfait.core.exception.BusinessException
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.text.ParseException
import java.util.Date

@Component
class AppleIdTokenVerifyAdapter(
    @Value("\${apple.issuer}") private val issuer: String,
    @Value("\${apple.client-id}") private val clientId: String,
    @Value("\${apple.jwks-uri}") jwksUri: String,
) : AppleIdTokenVerifyPort {
    private val log = LoggerFactory.getLogger(AppleIdTokenVerifyAdapter::class.java)

    private val jwtProcessor: ConfigurableJWTProcessor<SecurityContext> =
        DefaultJWTProcessor<SecurityContext>().apply {
            val keySource: JWKSource<SecurityContext> = RemoteJWKSet(URL(jwksUri))
            jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, keySource)
        }

    override fun verify(
        identityToken: String,
        nonce: String,
    ): String {
        val claims = parseAndVerifySignature(identityToken)
        return validateClaims(claims, nonce)
    }

    private fun parseAndVerifySignature(identityToken: String): JWTClaimsSet =
        try {
            jwtProcessor.process(identityToken, null)
        } catch (e: RemoteKeySourceException) {
            val cause = e.cause
            if (cause is IOException) {
                log.warn("애플 JWKS 서버 연결 실패", e)
                throw BusinessException(AuthErrorCode.APPLE_SERVER_UNAVAILABLE)
            }
            log.warn("애플 JWKS 응답이 비정상입니다", e)
            throw BusinessException(AuthErrorCode.APPLE_SERVER_ERROR)
        } catch (e: BadJOSEException) {
            log.warn("ID 토큰 서명 검증 실패", e)
            throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
        } catch (e: ParseException) {
            log.warn("ID 토큰 파싱 실패", e)
            throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
        } catch (e: JOSEException) {
            log.warn("애플 JWKS 조회 실패", e)
            throw BusinessException(AuthErrorCode.APPLE_SERVER_ERROR)
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
        val expectedNonce = sha256Hex(nonce)

        if (claims.issuer != issuer) {
            log.warn("ID 토큰 클레임 검증 실패: iss 불일치 (actual={}, expected={})", claims.issuer, issuer)
            throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
        }
        if (!claims.audience.contains(clientId)) {
            log.warn("ID 토큰 클레임 검증 실패: aud 불일치 (actual={}, expected={})", claims.audience, clientId)
            throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
        }
        if (actualNonce != expectedNonce) {
            log.warn("ID 토큰 클레임 검증 실패: nonce 불일치 (actual={}, expected={})", actualNonce, expectedNonce)
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

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val CLAIM_NONCE = "nonce"
    }
}
