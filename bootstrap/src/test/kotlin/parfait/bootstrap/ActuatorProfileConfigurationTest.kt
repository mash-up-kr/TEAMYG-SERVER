package parfait.bootstrap

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.Test
import kotlin.test.assertEquals

class ActuatorProfileConfigurationTest {
    @Test
    fun `공통 설정은 actuator health 엔드포인트만 노출한다`() {
        val properties = loadProperties("application.yaml")

        assertEquals("health", properties.getProperty("management.endpoints.web.exposure.include"))
    }

    @Test
    fun `local과 dev 프로필은 health 상세 정보를 노출한다`() {
        val local = loadProperties("application-local.yaml")
        val dev = loadProperties("application-dev.yaml")

        assertEquals("always", local.getProperty("management.endpoint.health.show-details"))
        assertEquals("always", dev.getProperty("management.endpoint.health.show-details"))
    }

    @Test
    fun `prod 프로필은 health 상세 정보를 노출하지 않는다`() {
        val prod = loadProperties("application-prod.yaml")

        assertEquals("never", prod.getProperty("management.endpoint.health.show-details"))
    }

    private fun loadProperties(fileName: String) =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .single()
}
