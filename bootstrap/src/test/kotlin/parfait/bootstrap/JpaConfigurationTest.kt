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

    /**
     * 스키마 소유권은 #110에서 Flyway로 넘어갔다. 셋 중 하나라도 되돌아가면 그 순간부터
     * 마이그레이션 누락을 Hibernate가 조용히 덮어써 드리프트가 다시 쌓인다.
     */
    @Test
    fun `스키마는 Flyway가 소유하고 Hibernate는 검증만 한다`() {
        val properties = loadProperties("application.yaml")

        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"))
        assertEquals(true, properties.getProperty("spring.flyway.enabled"))
        // 배치 메타테이블은 V12가 단독으로 만든다. always면 Flyway와 이중 초기화가 된다.
        assertEquals("never", properties.getProperty("spring.batch.jdbc.initialize-schema"))
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
