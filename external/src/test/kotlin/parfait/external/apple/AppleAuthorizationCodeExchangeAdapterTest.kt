package parfait.external.apple

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.DefaultResourceLoader
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.exception.BusinessException
import java.nio.file.Files
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppleAuthorizationCodeExchangeAdapterTest {
    private lateinit var server: MockWebServer
    private lateinit var generator: AppleClientSecretGenerator

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        val ecKey = ECKeyGenerator(Curve.P_256).keyID("test-kid").generate()
        val encoded = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(ecKey.toECPrivateKey().encoded)
        val pem = "-----BEGIN PRIVATE KEY-----\n$encoded\n-----END PRIVATE KEY-----\n"
        val keyFile = Files.createTempFile("apple-test-key", ".pem")
        Files.writeString(keyFile, pem)
        generator =
            AppleClientSecretGenerator(
                resourceLoader = DefaultResourceLoader(),
                privateKeyPath = "file:${keyFile.toAbsolutePath()}",
                teamId = "TEAM123",
                keyId = "KEY123",
                clientId = "com.example.app",
            )
    }

    @AfterEach
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun adapter(): AppleAuthorizationCodeExchangeAdapter =
        AppleAuthorizationCodeExchangeAdapter(
            appleClientSecretGenerator = generator,
            clientId = "com.example.app",
            tokenUri = server.url("/auth/token").toString(),
        )

    @Test
    fun `정상 응답이면 refresh_token을 반환한다`() {
        server.enqueue(
            MockResponse()
                .setBody("""{"access_token":"a","refresh_token":"apple-refresh-1","expires_in":3600}""")
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json"),
        )

        val refreshToken = adapter().exchange("auth-code-1")

        assertEquals("apple-refresh-1", refreshToken)
    }

    @Test
    fun `정상 응답이지만 refresh_token이 없으면 APPLE_SERVER_ERROR 예외를 던진다`() {
        server.enqueue(
            MockResponse()
                .setBody("""{"access_token":"a","expires_in":3600}""")
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json"),
        )

        val exception = assertFailsWith<BusinessException> { adapter().exchange("auth-code-1") }

        assertEquals(AuthErrorCode.APPLE_SERVER_ERROR, exception.errorCode)
    }

    @Test
    fun `4xx 응답이면 INVALID_ID_TOKEN 예외를 던진다`() {
        server.enqueue(
            MockResponse()
                .setBody("""{"error":"invalid_grant"}""")
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json"),
        )

        val exception = assertFailsWith<BusinessException> { adapter().exchange("bad-code") }

        assertEquals(AuthErrorCode.INVALID_ID_TOKEN, exception.errorCode)
    }

    @Test
    fun `5xx 응답이면 APPLE_SERVER_ERROR 예외를 던진다`() {
        server.enqueue(MockResponse().setResponseCode(500))

        val exception = assertFailsWith<BusinessException> { adapter().exchange("auth-code-1") }

        assertEquals(AuthErrorCode.APPLE_SERVER_ERROR, exception.errorCode)
    }

    @Test
    fun `애플 서버에 연결할 수 없으면 APPLE_SERVER_UNAVAILABLE 예외를 던진다`() {
        val exchangeAdapter = adapter()
        server.shutdown()

        val exception = assertFailsWith<BusinessException> { exchangeAdapter.exchange("auth-code-1") }

        assertEquals(AuthErrorCode.APPLE_SERVER_UNAVAILABLE, exception.errorCode)
    }
}
