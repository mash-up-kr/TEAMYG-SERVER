package parfait.persistence.report

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.persistence.TestApplication
import java.time.LocalDateTime

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class ReportSettingsAdapterTest {
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
    private lateinit var adapter: ReportSettingsAdapter

    @Test
    fun `마이그레이션이 넣어 둔 기본 설정을 읽는다`() {
        val settings = adapter.find()

        settings.adMode shouldBe false
        settings.intervalDays shouldBe 3
    }

    @Test
    fun `마지막 전송 시각을 저장하면 다시 읽을 수 있다`() {
        val sentAt = LocalDateTime.of(2026, 10, 30, 21, 0, 3)

        adapter.saveLastSentAt(sentAt)

        adapter.find().lastSentAt shouldBe sentAt
    }

    @Test
    fun `광고모드를 저장하면 다시 읽을 수 있다`() {
        adapter.saveAdMode(true)
        adapter.find().adMode shouldBe true

        adapter.saveAdMode(false)
        adapter.find().adMode shouldBe false
    }

    @Test
    fun `광고모드를 저장해도 마지막 전송 시각과 주기는 바뀌지 않는다`() {
        val sentAt = LocalDateTime.of(2026, 10, 30, 21, 0, 3)
        adapter.saveLastSentAt(sentAt)
        val before = adapter.find()

        try {
            adapter.saveAdMode(true)

            val after = adapter.find()
            after.adMode shouldBe true
            after.lastSentAt shouldBe sentAt
            after.intervalDays shouldBe before.intervalDays
        } finally {
            adapter.saveAdMode(false)
        }
    }

    @Test
    fun `마지막 전송 시각을 저장해도 광고모드는 바뀌지 않는다`() {
        adapter.saveAdMode(true)

        try {
            adapter.saveLastSentAt(LocalDateTime.of(2026, 10, 31, 21, 0, 0))

            adapter.find().adMode shouldBe true
        } finally {
            adapter.saveAdMode(false)
        }
    }
}
