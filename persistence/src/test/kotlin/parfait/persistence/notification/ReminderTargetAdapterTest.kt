package parfait.persistence.notification

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.core.notification.domain.DevicePlatform
import parfait.core.notification.domain.DeviceToken
import parfait.persistence.TestApplication
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.entity.ParfaitGroup
import parfait.persistence.entity.ParfaitGroupMember
import parfait.persistence.repository.DeviceTokenRepository
import parfait.persistence.repository.MemberRepository
import parfait.persistence.repository.ParfaitGroupMemberRepository
import parfait.persistence.repository.ParfaitGroupRepository
import java.time.LocalDateTime

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class ReminderTargetAdapterTest {
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

    @Autowired private lateinit var groupRepository: ParfaitGroupRepository

    @Autowired private lateinit var groupMemberRepository: ParfaitGroupMemberRepository

    @Autowired private lateinit var deviceTokenRepository: DeviceTokenRepository

    @Autowired private lateinit var deviceTokenAdapter: parfait.persistence.notification.DeviceTokenAdapter

    @Autowired private lateinit var adapter: ReminderTargetAdapter

    @AfterEach
    fun clear() {
        deviceTokenRepository.deleteAll()
        groupMemberRepository.deleteAll()
        groupRepository.deleteAll()
    }

    private fun newMember(tag: String): Long =
        memberRepository
            .save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "rt-$tag-${System.nanoTime()}",
                    globalNickname = "리마인드$tag",
                ),
            ).id!!

    private fun newGroup(code: String): Long =
        groupRepository.save(ParfaitGroup(name = "그룹$code", inviteCode = code, memberLimit = 12)).id!!

    private fun join(
        groupId: Long,
        memberId: Long,
        left: LocalDateTime? = null,
    ) {
        groupMemberRepository.save(
            ParfaitGroupMember(
                parfaitGroupId = groupId,
                memberId = memberId,
                groupNickname = "닉$memberId",
                leftAt = left,
            ),
        )
    }

    private fun addToken(
        memberId: Long,
        token: String,
    ) = deviceTokenAdapter.save(
        DeviceToken.register(memberId = memberId, sessionId = null, token = token, platform = DevicePlatform.ANDROID),
    )

    @Test
    fun `유효 멤버십 + 토큰 보유자의 토큰만, 그룹 N개여도 중복 없이 반환`() {
        val active = newMember("active")
        val g1 = newGroup("RTG001")
        val g2 = newGroup("RTG002")
        join(g1, active)
        join(g2, active)
        addToken(active, "tok-active")

        val leftOnly = newMember("left")
        val g3 = newGroup("RTG003")
        join(g3, leftOnly, left = LocalDateTime.now())
        addToken(leftOnly, "tok-left")

        val noGroup = newMember("nogroup")
        addToken(noGroup, "tok-nogroup")

        val noToken = newMember("notoken")
        join(newGroup("RTG004"), noToken)

        adapter.findActiveGroupMemberDeviceTokens() shouldContainExactlyInAnyOrder listOf("tok-active")
    }

    @Test
    fun `한 이용자가 기기 여러 대면 토큰이 각각 반환된다`() {
        val m = newMember("multi")
        join(newGroup("RTG010"), m)
        addToken(m, "tok-1")
        addToken(m, "tok-2")

        adapter.findActiveGroupMemberDeviceTokens() shouldContainExactlyInAnyOrder listOf("tok-1", "tok-2")
    }
}
