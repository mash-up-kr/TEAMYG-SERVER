package parfait.bootstrap

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.Test
import kotlin.test.assertEquals

class AppleConfigurationTest {
    @Test
    fun `공통 설정은 애플 issuer·jwks-uri·token-uri를 정의한다`() {
        val properties = loadProperties("application.yaml")

        assertEquals("https://appleid.apple.com", properties.getProperty("apple.issuer"))
        assertEquals("https://appleid.apple.com/auth/keys", properties.getProperty("apple.jwks-uri"))
        assertEquals("https://appleid.apple.com/auth/token", properties.getProperty("apple.token-uri"))
    }

    @Test
    fun `local 프로필은 클래스패스 테스트용 개인키 경로를 사용한다`() {
        val local = loadProperties("application-local.yaml")

        assertEquals(
            "classpath:apple/local-test-private-key.p8",
            local.getProperty("apple.private-key-path"),
        )
    }

    @Test
    fun `dev와 prod 프로필은 client-id·team-id·key-id를 환경변수로 위임하고 컨테이너 내부 고정 경로를 쓴다`() {
        val dev = loadProperties("application-dev.yaml")
        val prod = loadProperties("application-prod.yaml")

        for (properties in listOf(dev, prod)) {
            assertEquals("\${APPLE_CLIENT_ID}", properties.getProperty("apple.client-id"))
            assertEquals("\${APPLE_TEAM_ID}", properties.getProperty("apple.team-id"))
            assertEquals("\${APPLE_KEY_ID}", properties.getProperty("apple.key-id"))
            assertEquals("file:/app/apple-private-key.p8", properties.getProperty("apple.private-key-path"))
        }
    }

    private fun loadProperties(fileName: String) =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .single()
}
