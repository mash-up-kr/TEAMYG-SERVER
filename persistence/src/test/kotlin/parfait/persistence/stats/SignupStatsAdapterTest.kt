package parfait.persistence.stats

import io.kotest.assertions.throwables.shouldThrow
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
import parfait.core.stats.domain.SignupStats
import parfait.persistence.TestApplication
import java.time.LocalDateTime

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class SignupStatsAdapterTest {
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

    private lateinit var newActive: Number
    private lateinit var oldWithdrawn: Number

    @BeforeEach
    fun seed() {
        jdbcTemplate.update("DELETE FROM member")
        // 기간 내 가입, 현재도 활성 → 신규
        newActive = insert("a", created = "2026-10-28 10:00:00", deleted = null)
        // 예전에 가입, 기간 내 탈퇴 → 탈퇴
        oldWithdrawn = insert("b", created = "2026-10-10 10:00:00", deleted = "2026-10-29 10:00:00")
        // 기간 내 가입, 기간 내 탈퇴 → 신규 + 탈퇴 + 신규 중 탈퇴
        insert("c", created = "2026-10-28 11:00:00", deleted = "2026-10-29 11:00:00")
        // 예전에 가입, 활성 → 누적에만 포함
        insert("d", created = "2026-10-10 11:00:00", deleted = null)
        // 기간 이후 가입 → 아무 데도 포함되지 않음
        insert("e", created = "2026-10-31 10:00:00", deleted = null)
        // 예전에 가입, 기간 이후 탈퇴 → to 시점에는 남아 있으므로 누적에 포함
        insert("f", created = "2026-10-10 12:00:00", deleted = "2026-10-31 10:00:00")
    }

    private fun insert(
        providerUserId: String,
        created: String,
        deleted: String?,
    ): Number {
        jdbcTemplate.update(
            """
            INSERT INTO member (login_provider, provider_user_id, global_nickname, created_at, updated_at, deleted_at)
            VALUES ('KAKAO', ?, ?, ?, ?, ?)
            """.trimIndent(),
            providerUserId,
            "닉네임-$providerUserId",
            created,
            created,
            deleted,
        )
        return jdbcTemplate.queryForObject(
            "SELECT id FROM member WHERE provider_user_id = ?",
            Long::class.java,
            providerUserId,
        )!!
    }

    @Test
    fun `신규 가입 탈퇴 신규 중 탈퇴 누적을 집계한다`() {
        val adapter = SignupStatsAdapter(entityManager, excludedMemberIdsRaw = "")

        adapter.aggregate(from, to) shouldBe
            SignupStats(newSignups = 2, withdrawals = 2, newSignupWithdrawals = 1, totalUsers = 3)
    }

    @Test
    fun `제외 목록의 회원은 모든 수치에서 빠진다`() {
        val adapter =
            SignupStatsAdapter(
                entityManager,
                excludedMemberIdsRaw = "${newActive.toLong()}, ${oldWithdrawn.toLong()}",
            )

        // a(신규) 와 b(탈퇴) 를 제외 → 신규 1(c), 탈퇴 1(c), 신규 중 탈퇴 1(c), 누적 2(d, f)
        adapter.aggregate(from, to) shouldBe
            SignupStats(newSignups = 1, withdrawals = 1, newSignupWithdrawals = 1, totalUsers = 2)
    }

    @Test
    fun `해당 기간에 아무도 없으면 0이다`() {
        val adapter = SignupStatsAdapter(entityManager, excludedMemberIdsRaw = "")

        adapter.aggregate(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2020, 1, 2, 0, 0)) shouldBe
            SignupStats(newSignups = 0, withdrawals = 0, newSignupWithdrawals = 0, totalUsers = 0)
    }

    @Test
    fun `기간은 반열린 구간이라 from 시각은 포함하고 to 시각은 제외한다`() {
        jdbcTemplate.update("DELETE FROM member")
        insert("at-from", created = "2026-10-27 00:00:00", deleted = null)
        insert("at-to", created = "2026-10-30 00:00:00", deleted = null)
        val adapter = SignupStatsAdapter(entityManager, excludedMemberIdsRaw = "")

        // from 시각 가입 1명만 신규·누적에 포함, to 시각 가입은 어디에도 포함되지 않는다
        adapter.aggregate(from, to) shouldBe
            SignupStats(newSignups = 1, withdrawals = 0, newSignupWithdrawals = 0, totalUsers = 1)
    }

    @Test
    fun `제외 목록에 잘못된 값이 있으면 예외가 난다`() {
        shouldThrow<IllegalStateException> {
            SignupStatsAdapter(entityManager, excludedMemberIdsRaw = "1,abc")
        }
    }
}
