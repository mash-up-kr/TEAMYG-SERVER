package parfait.external.kakao

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
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val ISSUER = "https://kauth.kakao.com"
private const val APP_KEY = "test-app-key"
private const val KEY_ID = "test-kid"

class KakaoIdTokenVerifyAdapterTest {
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

    private fun adapter(): KakaoIdTokenVerifyAdapter =
        KakaoIdTokenVerifyAdapter(
            issuer = ISSUER,
            appKey = APP_KEY,
            jwksUri = server.url("/.well-known/jwks.json").toString(),
        )

    private fun enqueueJwks() {
        val jwkSet = JWKSet(rsaKey.toPublicJWK())
        val json = JSONObjectUtils.toJSONString(jwkSet.toJSONObject())
        server.enqueue(MockResponse().setBody(json).setResponseCode(200))
    }

    private fun signedToken(
        signingKey: RSAKey = rsaKey,
        issuer: String = ISSUER,
        audience: String = APP_KEY,
        nonce: String = "test-nonce",
        expiresInMillis: Long = 60_000,
    ): String {
        val claims =
            JWTClaimsSet
                .Builder()
                .subject("kakao-sub-123")
                .issuer(issuer)
                .audience(audience)
                .claim("nonce", nonce)
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

        val providerUserId = adapter().verify(token, "test-nonce")

        assertEquals("kakao-sub-123", providerUserId)
    }

    @Test
    fun `서명이 다른 키로 되어 있으면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val forgedKey = RSAKeyGenerator(2048).keyID(KEY_ID).generate()
        val token = signedToken(signingKey = forgedKey)

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, "test-nonce") }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `만료된 토큰이면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val token = signedToken(expiresInMillis = -1_000)

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, "test-nonce") }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `nonce가 일치하지 않으면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val token = signedToken(nonce = "issued-nonce")

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, "different-nonce") }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `aud가 일치하지 않으면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val token = signedToken(audience = "other-app-key")

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, "test-nonce") }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `iss가 일치하지 않으면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val token = signedToken(issuer = "https://evil.example.com")

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, "test-nonce") }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `JWKS 응답이 비정상이면 KAKAO_JWKS_FETCH_FAILED 예외를 던진다`() {
        server.enqueue(MockResponse().setBody("not-a-valid-jwks-json").setResponseCode(200))
        val token = signedToken()

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, "test-nonce") }

        assertEquals(AuthErrorCode.KAKAO_JWKS_FETCH_FAILED, exception.errorCode)
    }

    @Test
    fun `카카오 서버에 연결할 수 없으면 KAKAO_SERVER_UNAVAILABLE 예외를 던진다`() {
        val kakaoAdapter = adapter()
        val token = signedToken()
        server.shutdown()

        val exception = assertFailsWith<BusinessException> { kakaoAdapter.verify(token, "test-nonce") }

        assertEquals(AuthErrorCode.KAKAO_SERVER_UNAVAILABLE, exception.errorCode)
    }

    @Test
    fun `nonce 클레임이 문자열이 아니면 INVALID_ID_TOKEN 예외를 던진다`() {
        enqueueJwks()
        val claims =
            JWTClaimsSet
                .Builder()
                .subject("kakao-sub-123")
                .issuer(ISSUER)
                .audience(APP_KEY)
                .claim("nonce", 12345)
                .expirationTime(Date(System.currentTimeMillis() + 60_000))
                .build()
        val signedJWT = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims)
        signedJWT.sign(RSASSASigner(rsaKey))
        val token = signedJWT.serialize()

        val exception = assertFailsWith<BusinessException> { adapter().verify(token, "test-nonce") }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }
}
