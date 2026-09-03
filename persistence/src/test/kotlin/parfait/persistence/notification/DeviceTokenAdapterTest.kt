package parfait.persistence.notification

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
import parfait.persistence.repository.DeviceTokenRepository
import parfait.persistence.repository.MemberRepository

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class DeviceTokenAdapterTest {
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
    private lateinit var deviceTokenRepository: DeviceTokenRepository

    @Autowired
    private lateinit var memberRepository: MemberRepository

    // 파생 delete 쿼리(deleteByMemberIdAndSessionId)는 호출자 쪽 트랜잭션이 필요하다.
    // 어댑터를 직접 생성하면 클래스의 @Transactional이 적용되지 않으므로 스프링 빈을 주입받는다.
    @Autowired
    private lateinit var adapter: DeviceTokenAdapter

    // device_token.member_id는 member(id)를 참조하는 FK 제약(fk_device_token_member)이 걸려 있어,
    // 토큰을 저장하기 전에 실제 member 행이 있어야 한다. 테스트마다 새 member 두 개를 만들어 그 id를 쓴다.
    private var memberId1: Long = 0L
    private var memberId2: Long = 0L

    @BeforeEach
    fun setUp() {
        val suffix = System.nanoTime()
        memberId1 =
            memberRepository
                .save(
                    Member(
                        loginProvider = LoginProvider.KAKAO,
                        providerUserId = "device-token-test-1-$suffix",
                        globalNickname = "토큰테스터1",
                    ),
                ).id!!
        memberId2 =
            memberRepository
                .save(
                    Member(
                        loginProvider = LoginProvider.KAKAO,
                        providerUserId = "device-token-test-2-$suffix",
                        globalNickname = "토큰테스터2",
                    ),
                ).id!!
    }

    @AfterEach
    fun clear() {
        deviceTokenRepository.deleteAll()
    }

    @Test
    fun `save로 새 토큰을 저장하면 id가 부여되고 findByToken으로 조회된다`() {
        val saved =
            adapter.save(
                DeviceToken.register(
                    memberId = memberId1,
                    sessionId = "s1",
                    token = "tok-1",
                    platform = DevicePlatform.IOS,
                ),
            )

        saved.id.shouldNotBeNull()
        val found = adapter.findByToken("tok-1")
        found.shouldNotBeNull()
        found.memberId shouldBe memberId1
        found.sessionId shouldBe "s1"
        found.platform shouldBe DevicePlatform.IOS
    }

    @Test
    fun `findByToken은 없는 토큰이면 null을 반환한다`() {
        adapter.findByToken("no-such-token").shouldBeNull()
    }

    @Test
    fun `id를 가진 도메인을 save하면 새 행을 만들지 않고 기존 행을 갱신한다`() {
        adapter.save(
            DeviceToken.register(
                memberId = memberId1,
                sessionId = "s1",
                token = "tok-1",
                platform = DevicePlatform.IOS,
            ),
        )
        val existing = adapter.findByToken("tok-1")!!
        existing.reassign(memberId = memberId2, sessionId = "s2", platform = DevicePlatform.ANDROID)

        adapter.save(existing)

        deviceTokenRepository.count() shouldBe 1L
        val reloaded = adapter.findByToken("tok-1")!!
        reloaded.memberId shouldBe memberId2
        reloaded.sessionId shouldBe "s2"
        reloaded.platform shouldBe DevicePlatform.ANDROID
    }

    @Test
    fun `delete는 해당 세션 행을 지우고 없어도 예외가 없다`() {
        adapter.save(
            DeviceToken.register(
                memberId = memberId1,
                sessionId = "s1",
                token = "tok-1",
                platform = DevicePlatform.IOS,
            ),
        )

        adapter.delete(memberId = memberId1, sessionId = "no-session")
        adapter.findByToken("tok-1").shouldNotBeNull()

        adapter.delete(memberId = memberId2, sessionId = "s1")
        adapter.findByToken("tok-1").shouldNotBeNull()

        adapter.delete(memberId = memberId1, sessionId = "s1")
        adapter.findByToken("tok-1").shouldBeNull()

        adapter.delete(memberId = memberId1, sessionId = "s1")
    }

    @Test
    fun `deleteAllByMemberId는 해당 회원의 토큰만 지우고 없어도 예외가 없다`() {
        adapter.save(
            DeviceToken.register(
                memberId = memberId1,
                sessionId = "s1",
                token = "tok-1",
                platform = DevicePlatform.IOS,
            ),
        )
        adapter.save(
            DeviceToken.register(
                memberId = memberId2,
                sessionId = "s2",
                token = "tok-2",
                platform = DevicePlatform.ANDROID,
            ),
        )

        adapter.deleteAllByMemberId(memberId1)

        adapter.findByToken("tok-1").shouldBeNull()
        adapter.findByToken("tok-2").shouldNotBeNull()

        adapter.deleteAllByMemberId(memberId1)
    }

    @Test
    fun `findByMemberId 는 해당 회원의 토큰을 모두 반환한다`() {
        adapter.save(DeviceToken.register(memberId1, "s1", "tok-a", DevicePlatform.IOS))
        adapter.save(DeviceToken.register(memberId1, "s2", "tok-b", DevicePlatform.ANDROID))
        adapter.save(DeviceToken.register(memberId2, "s3", "tok-c", DevicePlatform.IOS))

        val tokens = adapter.findByMemberId(memberId1)

        tokens.map { it.token } shouldContainExactlyInAnyOrder listOf("tok-a", "tok-b")
    }

    @Test
    fun `deleteByToken 은 해당 토큰만 지우고 없어도 예외가 없다`() {
        adapter.save(DeviceToken.register(memberId1, "s1", "tok-a", DevicePlatform.IOS))
        adapter.save(DeviceToken.register(memberId1, "s2", "tok-b", DevicePlatform.ANDROID))

        adapter.deleteByToken("tok-a")

        adapter.findByToken("tok-a").shouldBeNull()
        adapter.findByToken("tok-b").shouldNotBeNull()
        adapter.deleteByToken("no-such")
    }
}
