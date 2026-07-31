package parfait.bootstrap

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthConfigurationTest {
    @Test
    fun `공통 설정은 access·refresh 토큰 만료시간을 정의한다`() {
        val properties = loadProperties("application.yaml")

        assertEquals(3600, properties.getProperty("jwt.access-token-expiration-seconds"))
        assertEquals(1_209_600, properties.getProperty("jwt.refresh-token-expiration-seconds"))
    }

    @Test
    fun `dev와 prod 프로필은 jwt secret-key와 redis 접속 정보를 환경변수로 위임한다`() {
        val dev = loadProperties("application-dev.yaml")
        val prod = loadProperties("application-prod.yaml")

        assertEquals("\${JWT_SECRET_KEY}", dev.getProperty("jwt.secret-key"))
        assertEquals("\${REDIS_HOST}", dev.getProperty("spring.data.redis.host"))
        assertEquals("\${REDIS_PORT}", dev.getProperty("spring.data.redis.port"))
        assertEquals("\${JWT_SECRET_KEY}", prod.getProperty("jwt.secret-key"))
        assertEquals("\${REDIS_HOST}", prod.getProperty("spring.data.redis.host"))
        assertEquals("\${REDIS_PORT}", prod.getProperty("spring.data.redis.port"))
    }

    private fun loadProperties(fileName: String) =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .single()
}
