package parfait.persistence.cache

import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Testcontainers
class RedisCanvasCacheAdapterTest {
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
    private val redisTemplate = StringRedisTemplate(connectionFactory).apply { afterPropertiesSet() }
    private val meterRegistry = SimpleMeterRegistry()
    private val adapter = RedisCanvasCacheAdapter(redisTemplate, meterRegistry)

    @AfterEach
    fun tearDown() {
        redisTemplate.connectionFactory
            ?.connection
            ?.serverCommands()
            ?.flushAll()
        connectionFactory.destroy()
    }

    @Test
    fun `캔버스 version 을 저장하면 읽을 수 있고 TTL 이 5분 이내로 설정된다`() {
        adapter.putParfaitVersion(5L, 42L)

        adapter.getParfaitVersion(5L) shouldBe 42L
        val ttl = redisTemplate.getExpire("parfait:version:5", TimeUnit.SECONDS)
        ttl shouldBeGreaterThan 0L
        ttl shouldBeLessThanOrEqual 300L
    }

    @Test
    fun `없는 키는 null 이고 evict 하면 사라진다`() {
        adapter.getParfaitVersion(404L) shouldBe null

        adapter.putParfaitVersion(5L, 42L)
        adapter.evictParfaitVersion(5L)

        adapter.getParfaitVersion(5L) shouldBe null
    }

    @Test
    fun `그룹 version 을 저장하고 지울 수 있다`() {
        adapter.putGroupVersion(1L, 3L)
        adapter.getGroupVersion(1L) shouldBe 3L

        adapter.evictGroupVersion(1L)

        adapter.getGroupVersion(1L) shouldBe null
    }

    @Test
    fun `오늘 parfaitId 는 날짜가 들어간 키로 저장한다`() {
        val date = LocalDate.of(2026, 10, 10)

        adapter.putTodayParfaitId(1L, date, 5L)

        adapter.getTodayParfaitId(1L, date) shouldBe 5L
        adapter.getTodayParfaitId(1L, date.plusDays(1)) shouldBe null
        redisTemplate.hasKey("parfait:today:1:2026-10-10") shouldBe true
    }

    @Test
    fun `오늘 parfaitId 를 evict 하면 그 날짜 키만 사라진다`() {
        val date = LocalDate.of(2026, 10, 10)
        adapter.putTodayParfaitId(1L, date, 5L)
        adapter.putTodayParfaitId(1L, date.plusDays(1), 6L)
        adapter.putTodayParfaitId(2L, date, 7L)

        adapter.evictTodayParfaitId(1L, date)

        redisTemplate.hasKey("parfait:today:1:2026-10-10") shouldBe false
        adapter.getTodayParfaitId(1L, date) shouldBe null
        adapter.getTodayParfaitId(1L, date.plusDays(1)) shouldBe 6L
        adapter.getTodayParfaitId(2L, date) shouldBe 7L
    }

    @Test
    fun `멤버는 긍정 결과만 저장하고 evict 하면 사라진다`() {
        adapter.isMember(1L, 42L) shouldBe false

        adapter.putMember(1L, 42L)
        adapter.isMember(1L, 42L) shouldBe true
        adapter.isMember(1L, 43L) shouldBe false

        adapter.evictMember(1L, 42L)
        adapter.isMember(1L, 42L) shouldBe false
    }

    @Test
    fun `조회 결과를 hit 과 miss 카운터로 센다`() {
        adapter.getParfaitVersion(5L)
        adapter.putParfaitVersion(5L, 1L)
        adapter.getParfaitVersion(5L)

        meterRegistry.counter("parfait.version.cache", "result", "miss", "key", "parfait_version").count() shouldBe 1.0
        meterRegistry.counter("parfait.version.cache", "result", "hit", "key", "parfait_version").count() shouldBe 1.0
    }

    @Test
    fun `Redis 에 연결할 수 없어도 예외 없이 miss 로 처리하고 쓰기와 삭제는 삼킨다`() {
        val deadFactory =
            LettuceConnectionFactory(RedisStandaloneConfiguration("localhost", 1)).apply { afterPropertiesSet() }
        val deadTemplate = StringRedisTemplate(deadFactory).apply { afterPropertiesSet() }
        val deadRegistry = SimpleMeterRegistry()
        val deadAdapter = RedisCanvasCacheAdapter(deadTemplate, deadRegistry)

        deadAdapter.getParfaitVersion(5L) shouldBe null
        deadAdapter.isMember(1L, 42L) shouldBe false
        deadAdapter.putParfaitVersion(5L, 1L)
        deadAdapter.evictParfaitVersion(5L)
        deadAdapter.evictTodayParfaitId(1L, LocalDate.of(2026, 10, 10))

        // Redis 장애는 miss(키 없음)와 구분해 error 로 센다.
        deadRegistry.counter("parfait.version.cache", "result", "error", "key", "parfait_version").count() shouldBe 1.0
        deadRegistry.counter("parfait.version.cache", "result", "error", "key", "member").count() shouldBe 1.0
        deadRegistry.counter("parfait.version.cache", "result", "miss", "key", "parfait_version").count() shouldBe 0.0

        deadFactory.destroy()
    }

    @Test
    fun `숫자가 아닌 값이 들어 있으면 null 을 돌려주고 hit 이 아니라 miss 로 센다`() {
        redisTemplate.opsForValue().set("parfait:version:5", "not-a-number")

        adapter.getParfaitVersion(5L) shouldBe null

        meterRegistry.counter("parfait.version.cache", "result", "miss", "key", "parfait_version").count() shouldBe 1.0
        meterRegistry.counter("parfait.version.cache", "result", "hit", "key", "parfait_version").count() shouldBe 0.0
    }
}
