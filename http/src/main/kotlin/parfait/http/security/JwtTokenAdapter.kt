package parfait.http.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import parfait.core.exception.AuthErrorCode
import parfait.core.exception.BusinessException
import parfait.core.port.out.RefreshTokenClaims
import parfait.core.port.out.TokenIssuePort
import parfait.core.port.out.TokenValidatePort
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenAdapter(
    @Value("\${jwt.secret-key}") secretKey: String,
    @Value("\${jwt.access-token-expiration-seconds}") private val accessTokenExpirationSeconds: Long,
    @Value("\${jwt.refresh-token-expiration-seconds}") private val refreshTokenExpirationSeconds: Long,
) : TokenIssuePort,
    TokenValidatePort {
    private val key: SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray())

    override fun createAccessToken(memberId: Long): String =
        Jwts
            .builder()
            .subject(memberId.toString())
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

    override fun validateAccessToken(token: String): Long {
        val claims = parseClaims(token)
        if (claims.get(CLAIM_TYPE, String::class.java) != TOKEN_TYPE_ACCESS) {
            throw BusinessException(AuthErrorCode.INVALID_TOKEN)
        }
        return claims.subject.toLong()
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
    }
}
