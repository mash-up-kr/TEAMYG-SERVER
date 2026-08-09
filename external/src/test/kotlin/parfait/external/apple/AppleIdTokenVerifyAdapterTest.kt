package parfait.external.apple

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.util.JSONObjectUtils
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.exception.BusinessException
import java.security.MessageDigest
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val ISSUER = "https://appleid.apple.com"
private const val CLIENT_ID = "com.example.app"
private const val KEY_ID = "test-kid"
private const val RAW_NONCE = "raw-nonce-value"

private fun sha256Hex(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

class AppleIdTokenVerifyAdapterTest {
    private lateinit var server: MockWebServer
    private lateinit var rsaKey: RSAKey

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        rsaKey = RSAKeyGenerator(2048).keyID(KEY_ID).generate()
    }

    @AfterEach
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun adapter(): AppleIdTokenVerifyAdapter =
        AppleIdTokenVerifyAdapter(
            issuer = ISSUER,
            clientId = CLIENT_ID,
            jwksUri = server.url("/auth/keys").toString(),
        )

    private fun enqueueJwks() {
        val jwkSet = JWKSet(rsaKey.toPublicJWK())
        val json = JSONObjectUtils.toJSONString(jwkSet.toJSONObject())
        server.enqueue(MockResponse().setBody(json).setResponseCode(200))
    }

    private fun signedToken(
        signingKey: RSAKey = rsaKey,
        issuer: String = ISSUER,
        audience: String = CLIENT_ID,
        nonceClaim: String = sha256Hex(RAW_NONCE),
        expiresInMillis: Long = 60_000,
    ): String {
        val claims =
            JWTClaimsSet
                .Builder()
                .subject("apple-sub-123")
                .issuer(issuer)
                .audience(audience)
                .claim("nonce", nonceClaim)
                .expirationTime(Date(System.currentTimeMillis() + expiresInMillis))
                .build()
        val signedJWT = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims)
        signedJWT.sign(RSASSASigner(signingKey))
        return signedJWT.serialize()
    }

    @Test
    fun `서명·클레임이 모두 유효하면 sub를 반환한다`() {
        enqueueJwks()
        val token = signedToken()

        val providerUserId = adapter().verify(token, RAW_NONCE)

        assertEquals("apple-sub-123", providerUserId)
    }

    @Test
    fun `서명이 다른 키로 되어 있으면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val forgedKey = RSAKeyGenerator(2048).keyID(KEY_ID).generate()
        val token = signedToken(signingKey = forgedKey)

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, RAW_NONCE) }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `만료된 토큰이면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val token = signedToken(expiresInMillis = -1_000)

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, RAW_NONCE) }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `nonce의 SHA-256 해시가 클레임과 다르면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val token = signedToken()

        val exception =
            assertFailsWith<BusinessException> { adapter().verify(token, "different-raw-nonce") }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `클레임의 nonce가 원본 값 그대로면(해시되지 않았으면) INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val token = signedToken(nonceClaim = RAW_NONCE)

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, RAW_NONCE) }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `aud가 일치하지 않으면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val token = signedToken(audience = "other-client-id")

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, RAW_NONCE) }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `iss가 일치하지 않으면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val token = signedToken(issuer = "https://evil.example.com")

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, RAW_NONCE) }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `JWKS 응답이 비정상이면 APPLE_SERVER_ERROR 예외를 던진다`() {
        server.enqueue(MockResponse().setBody("not-a-valid-jwks-json").setResponseCode(200))
        val token = signedToken()

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, RAW_NONCE) }

        assertEquals(AuthErrorCode.APPLE_SERVER_ERROR, exception.errorCode)
    }

    @Test
    fun `애플 서버에 연결할 수 없으면 APPLE_SERVER_UNAVAILABLE 예외를 던진다`() {
        val appleAdapter = adapter()
        val token = signedToken()
        server.shutdown()

        val exception = assertFailsWith<BusinessException> { appleAdapter.verify(token, RAW_NONCE) }

        assertEquals(AuthErrorCode.APPLE_SERVER_UNAVAILABLE, exception.errorCode)
    }

    @Test
    fun `nonce 클레임이 문자열이 아니면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val claims =
            JWTClaimsSet
                .Builder()
                .subject("apple-sub-123")
                .issuer(ISSUER)
                .audience(CLIENT_ID)
                .claim("nonce", 12345)
                .expirationTime(Date(System.currentTimeMillis() + 60_000))
                .build()
        val signedJWT = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims)
        signedJWT.sign(RSASSASigner(rsaKey))
        val token = signedJWT.serialize()

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, RAW_NONCE) }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }
}
