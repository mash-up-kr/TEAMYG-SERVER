package parfait.bootstrap

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.Test
import kotlin.test.assertEquals

class JpaConfigurationTest {
    @Test
    fun `공통 JPA 설정은 OSIV를 비활성화한다`() {
        val properties = loadProperties("application.yaml")

        assertEquals("local", properties.getProperty("spring.profiles.default"))
        assertEquals(false, properties.getProperty("spring.jpa.open-in-view"))
        assertEquals(false, properties.getProperty("spring.jpa.show-sql"))
        assertEquals(50, properties.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size"))
        assertEquals("Asia/Seoul", properties.getProperty("spring.jpa.properties.hibernate.jdbc.time_zone"))
    }

    @Test
    fun `local과 dev는 스키마를 갱신하고 prod는 검증만 한다`() {
        val local = loadProperties("application-local.yaml")
        val dev = loadProperties("application-dev.yaml")
        val prod = loadProperties("application-prod.yaml")

        assertEquals("update", local.getProperty("spring.jpa.hibernate.ddl-auto"))
        assertEquals("update", dev.getProperty("spring.jpa.hibernate.ddl-auto"))
        assertEquals("validate", prod.getProperty("spring.jpa.hibernate.ddl-auto"))
        assertEquals(false, local.getProperty("spring.datasource.url").toString().contains("createDatabaseIfNotExist"))
        assertEquals(false, dev.getProperty("spring.datasource.url").toString().contains("createDatabaseIfNotExist"))
    }

    private fun loadProperties(fileName: String) =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .single()
}
