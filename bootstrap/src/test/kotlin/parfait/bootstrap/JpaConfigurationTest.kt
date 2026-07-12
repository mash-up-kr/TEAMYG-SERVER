package parfait.bootstrap

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun `공통 설정은 Flyway가 스키마를 관리하고 Hibernate는 검증만 한다`() {
        val properties = loadProperties("application.yaml")

        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"))
        assertEquals(true, properties.getProperty("spring.flyway.enabled"))
        assertEquals("classpath:db/migration", properties.getProperty("spring.flyway.locations"))
    }

    @Test
    fun `프로필별 설정은 ddl-auto를 재정의하지 않는다`() {
        val local = loadProperties("application-local.yaml")
        val dev = loadProperties("application-dev.yaml")
        val prod = loadProperties("application-prod.yaml")

        assertNull(local.getProperty("spring.jpa.hibernate.ddl-auto"))
        assertNull(dev.getProperty("spring.jpa.hibernate.ddl-auto"))
        assertNull(prod.getProperty("spring.jpa.hibernate.ddl-auto"))
    }

    private fun loadProperties(fileName: String) =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .single()
}
