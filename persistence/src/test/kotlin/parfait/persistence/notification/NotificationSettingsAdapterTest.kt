package parfait.persistence.notification

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.core.notification.domain.NotificationSettings
import parfait.persistence.TestApplication
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.repository.MemberRepository

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class NotificationSettingsAdapterTest {
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

    @Autowired private lateinit var memberRepository: MemberRepository

    @Autowired private lateinit var adapter: NotificationSettingsAdapter

    private fun newMember(tag: String): Long =
        memberRepository
            .save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "ns-$tag-${System.nanoTime()}",
                    globalNickname = "설정$tag",
                ),
            ).id!!

    @Test
    fun `새 회원의 설정은 기본값이다`() {
        val id = newMember("default")

        adapter.findByMemberId(id) shouldBe
            NotificationSettings(dailyReminderEnabled = true, activityDigestEnabled = false)
    }

    @Test
    fun `daily 만 바꾸면 digest 는 기존 값 그대로다`() {
        val id = newMember("daily-only")
        adapter.update(id, null, true)

        adapter.update(id, false, null)

        adapter.findByMemberId(id) shouldBe
            NotificationSettings(dailyReminderEnabled = false, activityDigestEnabled = true)
    }

    @Test
    fun `digest 만 바꾸면 daily 는 기존 값 그대로다`() {
        val id = newMember("digest-only")

        adapter.update(id, null, true)

        adapter.findByMemberId(id) shouldBe
            NotificationSettings(dailyReminderEnabled = true, activityDigestEnabled = true)
    }

    @Test
    fun `둘 다 바꾸면 둘 다 저장되고 다시 조회된다`() {
        val id = newMember("both")

        adapter.update(id, false, true)

        adapter.findByMemberId(id) shouldBe
            NotificationSettings(dailyReminderEnabled = false, activityDigestEnabled = true)
    }

    @Test
    fun `없는 회원은 기본값을 돌려준다`() {
        adapter.findByMemberId(-1L) shouldBe NotificationSettings.DEFAULT
    }
}
