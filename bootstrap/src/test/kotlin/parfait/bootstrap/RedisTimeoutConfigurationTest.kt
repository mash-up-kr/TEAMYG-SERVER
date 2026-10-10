package parfait.bootstrap

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RedisTimeoutConfigurationTest {
    /**
     * Redis 클라이언트는 캔버스 캐시와 리프레시 토큰 어댑터가 공유한다. 기본값(60초)이면 Redis 지연 장애 때
     * 폴링·쓰기 요청이 오래 막히므로, 명령·연결 타임아웃을 짧게 고정한다.
     */
    @Test
    fun `공통 설정은 Redis 명령·연결 타임아웃을 500ms로 둔다`() {
        val properties = loadProperties("application.yaml")

        assertEquals("500ms", properties.getProperty("spring.data.redis.timeout"))
        assertEquals("500ms", properties.getProperty("spring.data.redis.connect-timeout"))
    }

    @Test
    fun `프로필별 설정은 Redis 타임아웃을 재정의하지 않는다`() {
        listOf("application-local.yaml", "application-dev.yaml", "application-prod.yaml").forEach { fileName ->
            val properties = loadProperties(fileName)

            assertNull(properties.getProperty("spring.data.redis.timeout"), fileName)
            assertNull(properties.getProperty("spring.data.redis.connect-timeout"), fileName)
        }
    }

    private fun loadProperties(fileName: String) =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .single()
}
