package parfait.external.apple

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date

@Component
class AppleClientSecretGenerator(
    resourceLoader: ResourceLoader,
    @Value("\${apple.private-key-path}") privateKeyPath: String,
    @Value("\${apple.team-id}") private val teamId: String,
    @Value("\${apple.key-id}") private val keyId: String,
    @Value("\${apple.client-id}") private val clientId: String,
) {
    private val signer: ECDSASigner =
        resourceLoader
            .getResource(privateKeyPath)
            .inputStream
            .bufferedReader()
            .readText()
            .let { pem -> parseEcPrivateKey(pem) }
            .let { privateKey -> ECDSASigner(privateKey) }

    private fun parseEcPrivateKey(pem: String): ECPrivateKey {
        val base64Body =
            pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")
        val der = Base64.getDecoder().decode(base64Body)
        val keySpec = PKCS8EncodedKeySpec(der)
        return KeyFactory.getInstance("EC").generatePrivate(keySpec) as ECPrivateKey
    }

    fun generate(): String {
        val now = Date()
        val claims =
            JWTClaimsSet
                .Builder()
                .issuer(teamId)
                .issueTime(now)
                .expirationTime(Date(now.time + EXPIRATION_MILLIS))
                .audience(AUDIENCE)
                .subject(clientId)
                .build()
        val signedJWT = SignedJWT(JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build(), claims)
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }

    companion object {
        private const val AUDIENCE = "https://appleid.apple.com"
        private const val EXPIRATION_MILLIS = 300_000L
    }
}
