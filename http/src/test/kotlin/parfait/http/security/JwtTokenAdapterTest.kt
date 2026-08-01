package parfait.http.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.exception.BusinessException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val TEST_SECRET_KEY = "test-secret-key-for-jwt-unit-test-32bytes-long!!"
private const val OTHER_SECRET_KEY = "forged-secret-key-that-is-at-least-32-bytes-long"

class JwtTokenAdapterTest {
    private val adapter =
        JwtTokenAdapter(
            secretKey = TEST_SECRET_KEY,
            accessTokenExpirationSeconds = 3600,
            refreshTokenExpirationSeconds = 1_209_600,
            registrationTokenExpirationSeconds = 600,
        )

    @Test
    fun `액세스 토큰을 발급하고 검증하면 같은 memberId를 반환한다`() {
        val accessToken = adapter.createAccessToken(memberId = 42L)

        val memberId = adapter.validateAccessToken(accessToken)

        assertEquals(42L, memberId)
    }

    @Test
    fun `리프레시 토큰을 발급하고 검증하면 memberId와 sessionId를 반환한다`() {
        val refreshToken = adapter.createRefreshToken(memberId = 42L, sessionId = "session-1")

        val claims = adapter.validateRefreshToken(refreshToken)

        assertEquals(42L, claims.memberId)
        assertEquals("session-1", claims.sessionId)
    }

    @Test
    fun `서명이 다른 토큰은 INVALID_TOKEN 예외를 던진다`() {
        val forgedToken =
            Jwts
                .builder()
                .subject("42")
                .signWith(Keys.hmacShaKeyFor(OTHER_SECRET_KEY.toByteArray()), Jwts.SIG.HS256)
                .compact()

        val exception =
            assertFailsWith<BusinessException> {
                adapter.validateAccessToken(forgedToken)
            }
        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `만료된 토큰은 EXPIRED_TOKEN 예외를 던진다`() {
        val expiredAdapter =
            JwtTokenAdapter(
                secretKey = TEST_SECRET_KEY,
                accessTokenExpirationSeconds = -5,
                refreshTokenExpirationSeconds = -5,
                registrationTokenExpirationSeconds = -5,
            )
        val expiredToken = expiredAdapter.createAccessToken(memberId = 42L)

        val exception =
            assertFailsWith<BusinessException> {
                adapter.validateAccessToken(expiredToken)
            }
        assertEquals(AuthErrorCode.EXPIRED_TOKEN, exception.errorCode)
    }

    @Test
    fun `액세스 토큰을 validateRefreshToken에 넣으면 INVALID_TOKEN 예외를 던진다`() {
        val accessToken = adapter.createAccessToken(memberId = 42L)

        val exception =
            assertFailsWith<BusinessException> {
                adapter.validateRefreshToken(accessToken)
            }
        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `리프레시 토큰을 validateAccessToken에 넣으면 INVALID_TOKEN 예외를 던진다`() {
        val refreshToken = adapter.createRefreshToken(memberId = 42L, sessionId = "session-1")

        val exception =
            assertFailsWith<BusinessException> {
                adapter.validateAccessToken(refreshToken)
            }
        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `빈 문자열 토큰을 validateAccessToken에 넣으면 INVALID_TOKEN 예외를 던진다`() {
        val exception =
            assertFailsWith<BusinessException> {
                adapter.validateAccessToken("")
            }
        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `형식이 깨진 토큰을 validateAccessToken에 넣으면 INVALID_TOKEN 예외를 던진다`() {
        val exception =
            assertFailsWith<BusinessException> {
                adapter.validateAccessToken("not-a-valid-jwt-token")
            }
        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `registrationToken을 발급하면 provider, providerUserId, purpose 클레임을 담는다`() {
        val token = adapter.createRegistrationToken(LoginProvider.KAKAO, "kakao-sub-1")

        val claims =
            Jwts
                .parser()
                .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.toByteArray()))
                .build()
                .parseSignedClaims(token)
                .payload

        assertEquals("KAKAO", claims.get("provider", String::class.java))
        assertEquals("kakao-sub-1", claims.get("providerUserId", String::class.java))
        assertEquals("REGISTRATION", claims.get("purpose", String::class.java))
    }

    @Test
    fun `만료된 registrationToken은 검증 시 EXPIRED_TOKEN 예외를 던진다`() {
        val expiredAdapter =
            JwtTokenAdapter(
                secretKey = TEST_SECRET_KEY,
                accessTokenExpirationSeconds = 3600,
                refreshTokenExpirationSeconds = 1_209_600,
                registrationTokenExpirationSeconds = -5,
            )
        val expiredToken = expiredAdapter.createRegistrationToken(LoginProvider.KAKAO, "kakao-sub-1")

        val exception =
            assertFailsWith<io.jsonwebtoken.ExpiredJwtException> {
                Jwts
                    .parser()
                    .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.toByteArray()))
                    .build()
                    .parseSignedClaims(expiredToken)
            }
        assertEquals(true, exception.message!!.contains("expired"))
    }
}
