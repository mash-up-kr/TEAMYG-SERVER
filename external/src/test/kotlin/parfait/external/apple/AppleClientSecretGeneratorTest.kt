package parfait.external.apple

import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jwt.SignedJWT
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.DefaultResourceLoader
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun toPkcs8Pem(ecKey: ECKey): String {
    val encoded = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(ecKey.toECPrivateKey().encoded)
    return "-----BEGIN PRIVATE KEY-----\n$encoded\n-----END PRIVATE KEY-----\n"
}

class AppleClientSecretGeneratorTest {
    @Test
    fun `team-id, client-id, key-id를 담아 개인키로 서명된 ES256 JWT를 생성한다`(
        @TempDir tempDir: Path,
    ) {
        val ecKey = ECKeyGenerator(Curve.P_256).keyID("test-kid").generate()
        val keyFile = tempDir.resolve("test-key.pem")
        keyFile.writeText(toPkcs8Pem(ecKey))

        val generator =
            AppleClientSecretGenerator(
                resourceLoader = DefaultResourceLoader(),
                privateKeyPath = "file:${keyFile.toAbsolutePath()}",
                teamId = "TEAM123",
                keyId = "KEY123",
                clientId = "com.example.app",
            )

        val jwt = generator.generate()
        val signedJWT = SignedJWT.parse(jwt)
        assertTrue(signedJWT.verify(ECDSAVerifier(ecKey.toECPublicKey())))
        assertEquals("TEAM123", signedJWT.jwtClaimsSet.issuer)
        assertEquals("com.example.app", signedJWT.jwtClaimsSet.subject)
        assertEquals(listOf("https://appleid.apple.com"), signedJWT.jwtClaimsSet.audience)
        assertEquals("KEY123", signedJWT.header.keyID)
        assertEquals("ES256", signedJWT.header.algorithm.name)
    }
}
