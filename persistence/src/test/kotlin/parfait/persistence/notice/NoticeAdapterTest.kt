package parfait.persistence.notice

import io.kotest.matchers.shouldBe
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
import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.domain.NoticeRevisionTarget
import parfait.core.notice.domain.NoticeType
import parfait.persistence.TestApplication
import java.time.LocalDate
import java.time.LocalDateTime

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class NoticeAdapterTest {
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
    private lateinit var adapter: NoticeAdapter

    private val today = LocalDate.of(2026, 11, 9)

    @BeforeEach
    fun clean() {
        jdbcTemplate.update("DELETE FROM notice")
    }

    private fun notice(
        platform: NoticePlatform = NoticePlatform.COMMON,
        publishedDate: LocalDate = today,
        title: String = "파르페 v1.0.0 업데이트 안내",
    ): Notice =
        Notice(
            type = NoticeType.UPDATE,
            platform = platform,
            version = "1.0.0",
            revisionTarget = null,
            title = title,
            content = "내용",
            publishedDate = publishedDate,
            createdAt = LocalDateTime.of(2026, 11, 9, 10, 0),
        )

    @Test
    fun `findVisible은 요청 플랫폼과 COMMON만 돌려주고 반대 플랫폼은 제외한다`() {
        adapter.save(notice(platform = NoticePlatform.IOS, title = "ios"))
        adapter.save(notice(platform = NoticePlatform.ANDROID, title = "android"))
        adapter.save(notice(platform = NoticePlatform.COMMON, title = "common"))

        val result = adapter.findVisible(NoticePlatform.IOS, today)

        result.size shouldBe 2
        result.map { it.title }.toSet() shouldBe setOf("ios", "common")
    }

    @Test
    fun `findVisible은 게시일이 미래인 공지를 제외한다`() {
        adapter.save(notice(publishedDate = today, title = "today"))
        adapter.save(notice(publishedDate = today.plusDays(1), title = "future"))

        val result = adapter.findVisible(NoticePlatform.IOS, today)

        result.map { it.title } shouldBe listOf("today")
    }

    @Test
    fun `findVisible은 게시일 내림차순, 같은 날은 id 내림차순으로 정렬한다`() {
        adapter.save(notice(publishedDate = today.minusDays(1), title = "older"))
        val first = adapter.save(notice(title = "same-day-first"))
        val second = adapter.save(notice(title = "same-day-second"))

        val result = adapter.findVisible(NoticePlatform.ANDROID, today)

        result.map { it.title } shouldBe listOf("same-day-second", "same-day-first", "older")
        (second.id!! > first.id!!) shouldBe true
    }

    @Test
    fun `save 후 findById로 모든 필드가 그대로 복원된다`() {
        val guide =
            Notice(
                type = NoticeType.GUIDE,
                platform = NoticePlatform.IOS,
                version = null,
                revisionTarget = NoticeRevisionTarget.BOTH,
                title = "이용약관 및 개인정보처리방침 개정 안내",
                content = "첫 줄\n둘째 줄\n\n셋째 줄",
                publishedDate = today,
                createdAt = LocalDateTime.of(2026, 11, 9, 10, 30, 15),
            )
        val update = notice(platform = NoticePlatform.ANDROID)

        val savedGuide = adapter.save(guide)
        val savedUpdate = adapter.save(update)

        adapter.findById(savedGuide.id!!) shouldBe guide.copy(id = savedGuide.id)
        adapter.findById(savedUpdate.id!!) shouldBe update.copy(id = savedUpdate.id)
    }

    @Test
    fun `findById는 없으면 null`() {
        adapter.findById(999_999L) shouldBe null
    }
}
