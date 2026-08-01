package parfait.bootstrap

import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import parfait.core.member.port.out.MemberQueryPort
import parfait.persistence.member.MemberAdapter

/**
 * `bootstrap`이 core/http/persistence/external/batch 전체를 실제로 조립했을 때
 * 예외 없이 뜨는지, 그리고 [MemberQueryPort]가 실제 [MemberAdapter]로 해석되는지만 검증하는 스모크 테스트.
 */
@Testcontainers
@SpringBootTest(
    classes = [ParfaitApplication::class],
    properties = [
        "jwt.secret-key=smoke-test-only-dummy-jwt-secret-key-32-bytes-min",
    ],
)
class ApplicationContextSmokeTest {
    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.4")

        @Container
        @JvmStatic
        val redis: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }

    @Autowired
    private lateinit var memberQueryPort: MemberQueryPort

    @Test
    fun `애플리케이션 컨텍스트가 예외 없이 기동되고 MemberQueryPort는 MemberAdapter로 해석된다`() {
        memberQueryPort.shouldBeInstanceOf<MemberAdapter>()
    }
}
