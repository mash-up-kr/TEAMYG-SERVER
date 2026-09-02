package parfait.bootstrap

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.Test
import kotlin.test.assertEquals

class FcmConfigurationTest {
    @Test
    fun `공통 설정은 fcm credentials-path 를 classpath 가짜 키로 둔다`() {
        val properties = loadProperties("application.yaml")

        assertEquals(
            "classpath:fcm/local-firebase-key.json",
            properties.getProperty("fcm.credentials-path"),
        )
    }

    @Test
    fun `dev 와 prod 프로필은 fcm credentials-path 를 컨테이너 파일 경로로 둔다`() {
        val dev = loadProperties("application-dev.yaml")
        val prod = loadProperties("application-prod.yaml")

        for (properties in listOf(dev, prod)) {
            assertEquals(
                "file:/app/parfait-firebase-key.json",
                properties.getProperty("fcm.credentials-path"),
            )
        }
    }

    private fun loadProperties(fileName: String) =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .single()
}
