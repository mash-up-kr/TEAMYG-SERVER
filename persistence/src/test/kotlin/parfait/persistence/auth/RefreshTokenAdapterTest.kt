package parfait.persistence.auth

import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class RefreshTokenAdapterTest {
    companion object {
        @Container
        @JvmStatic
        val redis: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }

    private val connectionFactory =
        LettuceConnectionFactory(RedisStandaloneConfiguration(redis.host, redis.getMappedPort(6379))).apply {
            afterPropertiesSet()
        }
    private val redisTemplate =
        StringRedisTemplate(connectionFactory).apply {
            afterPropertiesSet()
        }
    private val adapter = RefreshTokenAdapter(redisTemplate)

    @AfterEach
    fun destroyConnectionFactory() {
        connectionFactory.destroy()
    }

    @Test
    fun `refresh token을 저장하면 memberId와 sessionId로 조회할 수 있다`() {
        adapter.save(memberId = 1L, sessionId = "session-1", refreshToken = "refresh-token-value", ttlSeconds = 60)

        redisTemplate.opsForValue().get("refresh:1:session-1") shouldBe "refresh-token-value"
    }

    @Test
    fun `저장한 키에는 TTL이 설정된다`() {
        adapter.save(memberId = 1L, sessionId = "session-2", refreshToken = "refresh-token-value", ttlSeconds = 60)

        val ttl = redisTemplate.getExpire("refresh:1:session-2")

        ttl shouldBeGreaterThan 0L
        ttl shouldBeLessThanOrEqual 60L
    }

    @Test
    fun `저장된 refresh token을 memberId와 sessionId로 조회할 수 있다`() {
        adapter.save(memberId = 2L, sessionId = "session-3", refreshToken = "refresh-token-value", ttlSeconds = 60)

        adapter.findRefreshToken(memberId = 2L, sessionId = "session-3") shouldBe "refresh-token-value"
    }

    @Test
    fun `저장된 값이 없으면 조회시 null을 반환한다`() {
        adapter.findRefreshToken(memberId = 999L, sessionId = "no-such-session") shouldBe null
    }

    @Test
    fun `삭제하면 더 이상 조회되지 않는다`() {
        adapter.save(memberId = 3L, sessionId = "session-4", refreshToken = "refresh-token-value", ttlSeconds = 60)

        adapter.delete(memberId = 3L, sessionId = "session-4")

        adapter.findRefreshToken(memberId = 3L, sessionId = "session-4") shouldBe null
    }

    @Test
    fun `저장된 적 없는 세션을 삭제해도 예외가 발생하지 않는다`() {
        adapter.delete(memberId = 999L, sessionId = "no-such-session")
    }
}
