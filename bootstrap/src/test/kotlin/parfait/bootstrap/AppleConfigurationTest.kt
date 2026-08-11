package parfait.bootstrap

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.Test
import kotlin.test.assertEquals

class AppleConfigurationTest {
    @Test
    fun `공통 설정은 애플 issuer·jwks-uri를 정의한다`() {
        val properties = loadProperties("application.yaml")

        assertEquals("https://appleid.apple.com", properties.getProperty("apple.issuer"))
        assertEquals("https://appleid.apple.com/auth/keys", properties.getProperty("apple.jwks-uri"))
    }

    @Test
    fun `dev와 prod 프로필은 client-id를 환경변수로 위임한다`() {
        val dev = loadProperties("application-dev.yaml")
        val prod = loadProperties("application-prod.yaml")

        for (properties in listOf(dev, prod)) {
            assertEquals("\${APPLE_CLIENT_ID}", properties.getProperty("apple.client-id"))
        }
    }

    private fun loadProperties(fileName: String) =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .single()
}
