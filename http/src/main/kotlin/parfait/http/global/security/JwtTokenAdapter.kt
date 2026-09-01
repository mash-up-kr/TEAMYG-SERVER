// http/src/main/kotlin/parfait/http/global/security/JwtTokenAdapter.kt
package parfait.http.global.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.out.AccessTokenClaims
import parfait.core.auth.port.out.RefreshTokenClaims
import parfait.core.auth.port.out.RegistrationTokenClaims
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.auth.port.out.TokenValidatePort
import parfait.core.exception.BusinessException
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenAdapter(
    @Value("\${jwt.secret-key}") secretKey: String,
    @Value("\${jwt.access-token-expiration-seconds}") private val accessTokenExpirationSeconds: Long,
    @Value("\${jwt.refresh-token-expiration-seconds}") private val refreshTokenExpirationSeconds: Long,
    @Value("\${jwt.registration-token-expiration-seconds}") private val registrationTokenExpirationSeconds: Long,
) : TokenIssuePort,
    TokenValidatePort {
    private val key: SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray())

    override fun createAccessToken(
        memberId: Long,
        sessionId: String,
    ): String =
        Jwts
            .builder()
            .subject(memberId.toString())
            .claim(CLAIM_SESSION_ID, sessionId)
            .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + accessTokenExpirationSeconds * 1000))
            .signWith(key, Jwts.SIG.HS256)
            .compact()

    override fun createRefreshToken(
        memberId: Long,
        sessionId: String,
    ): String =
        Jwts
            .builder()
            .subject(memberId.toString())
            .claim(CLAIM_SESSION_ID, sessionId)
            .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + refreshTokenExpirationSeconds * 1000))
            .signWith(key, Jwts.SIG.HS256)
            .compact()

    override fun createRegistrationToken(
        provider: LoginProvider,
        providerUserId: String,
    ): String =
        Jwts
            .builder()
            .claim(CLAIM_PROVIDER, provider.name)
            .claim(CLAIM_PROVIDER_USER_ID, providerUserId)
            .claim(CLAIM_PURPOSE, PURPOSE_REGISTRATION)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + registrationTokenExpirationSeconds * 1000))
            .signWith(key, Jwts.SIG.HS256)
            .compact()

    override fun validateAccessToken(token: String): AccessTokenClaims {
        val claims = parseClaims(token)
        if (claims.get(CLAIM_TYPE, String::class.java) != TOKEN_TYPE_ACCESS) {
            throw BusinessException(AuthErrorCode.INVALID_TOKEN)
        }
        return AccessTokenClaims(
            memberId = claims.subject.toLong(),
            sessionId = claims.get(CLAIM_SESSION_ID, String::class.java),
        )
    }

    override fun validateRefreshToken(token: String): RefreshTokenClaims {
        val claims = parseClaims(token)
        if (claims.get(CLAIM_TYPE, String::class.java) != TOKEN_TYPE_REFRESH) {
            throw BusinessException(AuthErrorCode.INVALID_TOKEN)
        }
        return RefreshTokenClaims(
            memberId = claims.subject.toLong(),
            sessionId = claims.get(CLAIM_SESSION_ID, String::class.java),
        )
    }

    override fun validateRegistrationToken(token: String): RegistrationTokenClaims {
        val claims = parseClaims(token)
        if (claims.get(CLAIM_PURPOSE, String::class.java) != PURPOSE_REGISTRATION) {
            throw BusinessException(AuthErrorCode.INVALID_TOKEN)
        }
        return RegistrationTokenClaims(
            provider = LoginProvider.valueOf(claims.get(CLAIM_PROVIDER, String::class.java)),
            providerUserId = claims.get(CLAIM_PROVIDER_USER_ID, String::class.java),
        )
    }

    private fun parseClaims(token: String): Claims =
        try {
            Jwts
                .parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            throw BusinessException(AuthErrorCode.EXPIRED_TOKEN)
        } catch (e: JwtException) {
            throw BusinessException(AuthErrorCode.INVALID_TOKEN)
        } catch (e: IllegalArgumentException) {
            throw BusinessException(AuthErrorCode.INVALID_TOKEN)
        }

    companion object {
        private const val CLAIM_TYPE = "type"
        private const val TOKEN_TYPE_ACCESS = "ACCESS"
        private const val TOKEN_TYPE_REFRESH = "REFRESH"
        private const val CLAIM_SESSION_ID = "sessionId"
        private const val CLAIM_PROVIDER = "provider"
        private const val CLAIM_PROVIDER_USER_ID = "providerUserId"
        private const val CLAIM_PURPOSE = "purpose"
        private const val PURPOSE_REGISTRATION = "REGISTRATION"
    }
}
