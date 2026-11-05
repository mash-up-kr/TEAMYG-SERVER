package parfait.persistence.stats

import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.persistence.TestApplication
import java.time.LocalDateTime

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class GroupStatsAdapterTest {
    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.4")

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
        }
    }

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var entityManager: EntityManager

    private val from = LocalDateTime.of(2026, 10, 27, 0, 0)
    private val to = LocalDateTime.of(2026, 10, 30, 0, 0)

    @BeforeEach
    fun seed() {
        jdbcTemplate.update("DELETE FROM parfait_group")
    }

    private fun insertGroup(
        inviteCode: String,
        created: String,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO parfait_group (name, invite_code, member_limit, created_at, updated_at)
            VALUES (?, ?, 10, ?, ?)
            """.trimIndent(),
            "그룹-$inviteCode",
            inviteCode,
            created,
            created,
        )
    }

    @Test
    fun `구간 안에 생성된 그룹만 센다`() {
        insertGroup("AAAAAA", "2026-10-26 23:59:59") // 구간 이전
        insertGroup("BBBBBB", "2026-10-27 00:00:00") // from 포함
        insertGroup("CCCCCC", "2026-10-28 12:00:00")
        insertGroup("DDDDDD", "2026-10-30 00:00:00") // to 제외

        GroupStatsAdapter(entityManager).countCreated(from, to) shouldBe 2L
    }

    @Test
    fun `그룹이 없으면 0이다`() {
        GroupStatsAdapter(entityManager).countCreated(from, to) shouldBe 0L
    }
}
