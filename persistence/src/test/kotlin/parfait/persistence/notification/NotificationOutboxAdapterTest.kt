package parfait.persistence.notification

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.core.notification.domain.NotificationOutbox
import parfait.core.notification.domain.OutboxStatus
import parfait.core.notification.domain.ToppingPlacedPayload
import parfait.persistence.TestApplication
import parfait.persistence.repository.NotificationOutboxRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class NotificationOutboxAdapterTest {
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

    // mark* 는 어댑터 클래스의 @Transactional 더티 체킹으로 UPDATE 를 flush 하므로
    // 어댑터를 직접 생성하지 않고 스프링 빈을 주입받는다.
    @Autowired
    private lateinit var adapter: NotificationOutboxAdapter

    @Autowired
    private lateinit var repository: NotificationOutboxRepository

    private val base = LocalDateTime.of(2026, 9, 2, 10, 0, 0)
    private val payload =
        ToppingPlacedPayload(
            groupId = 1L,
            parfaitId = 5L,
            parfaitDate = LocalDate.of(2026, 9, 2),
            actorMemberId = 7L,
        )

    @AfterEach
    fun clean() {
        repository.deleteAll()
    }

    private fun pending(
        toppingId: Long,
        receiver: Long,
        scheduledAt: LocalDateTime,
    ) = NotificationOutbox.toppingPlaced(toppingId, receiver, payload, scheduledAt)

    @Test
    fun `saveAll 후 payload 가 JSON 으로 왕복되고 claimBatch 가 due 행만 준다`() {
        adapter.saveAll(
            listOf(
                pending(5L, 42L, base.minusMinutes(1)), // due
                pending(5L, 43L, base.plusMinutes(10)), // not due
            ),
        )

        val claimed = adapter.claimBatch(limit = 10, now = base)

        claimed shouldHaveSize 1
        claimed.single().receiverMemberId shouldBe 42L
        claimed.single().payload shouldBe payload
        claimed.single().status shouldBe OutboxStatus.PENDING
    }

    @Test
    fun `saveAll 은 이미 큐잉된 dedup_key 는 건너뛰고 새 수신자만 넣는다`() {
        adapter.saveAll(listOf(pending(5L, 42L, base)))
        adapter.saveAll(
            listOf(
                pending(5L, 42L, base), // 같은 toppingId+receiver → 중복 dedup_key, 스킵
                pending(5L, 43L, base), // 신규 dedup_key, 삽입
            ),
        )

        repository.count() shouldBe 2L
        repository.findAll().map { it.receiverMemberId }.toSet() shouldBe setOf(42L, 43L)
    }

    @Test
    fun `markRetry 후에는 scheduled_at 이 미래라 claimBatch 에서 빠진다`() {
        adapter.saveAll(listOf(pending(5L, 42L, base.minusMinutes(1))))
        val id = adapter.claimBatch(10, base).single().id!!

        adapter.markRetry(id, attempts = 1, scheduledAt = base.plusMinutes(5), error = "x")

        adapter.claimBatch(10, base) shouldHaveSize 0
        adapter.claimBatch(10, base.plusMinutes(6)) shouldHaveSize 1
    }

    @Test
    fun `markSent 는 상태와 sent_at, note 를 남긴다`() {
        adapter.saveAll(listOf(pending(5L, 42L, base.minusMinutes(1))))
        val id = adapter.claimBatch(10, base).single().id!!

        adapter.markSent(id, now = base, note = "NO_DEVICE_TOKEN")

        val e = repository.findById(id).get()
        e.status shouldBe OutboxStatus.SENT
        e.sentAt shouldBe base
        e.lastError shouldBe "NO_DEVICE_TOKEN"
    }

    @Test
    fun `deleteTerminalBefore 는 PENDING 을 건드리지 않고 오래된 종료 행만 지운다`() {
        adapter.saveAll(
            listOf(
                pending(5L, 42L, base), // PENDING
                pending(5L, 43L, base),
                pending(5L, 44L, base),
            ),
        )
        val ids = repository.findAll().sortedBy { it.receiverMemberId }
        adapter.markSent(ids[1].id!!, base, null) // receiver 43 → SENT
        adapter.markFailed(ids[2].id!!, "x") // receiver 44 → FAILED
        repository.saveAll(
            repository.findAll().map { it.also { row -> row.createdAt = base.minusDays(10) } },
        )

        val deleted = adapter.deleteTerminalBefore(base.minusDays(7))

        deleted shouldBe 2
        repository.count() shouldBe 1L
        repository.findAll().single().status shouldBe OutboxStatus.PENDING
    }
}
