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

    @Test
    fun `공통 설정은 notification outbox 폴 주기, 보관일, purge cron 을 정의한다`() {
        val properties = loadProperties("application.yaml")

        assertEquals("2000", properties.getProperty("notification.outbox.poll-interval-ms").toString())
        assertEquals("7", properties.getProperty("notification.outbox.retention-days").toString())
        assertEquals("0 0 4 * * *", properties.getProperty("notification.outbox.purge-cron"))
    }

    private fun loadProperties(fileName: String) =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .single()
}
