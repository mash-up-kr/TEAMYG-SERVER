package parfait.persistence.member

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.persistence.TestApplication
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.repository.MemberRepository
import parfait.core.auth.domain.LoginProvider as CoreLoginProvider

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class MemberAdapterTest {
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
    private lateinit var memberRepository: MemberRepository

    @Test
    fun `memberRepository의 existsById 결과를 그대로 반환한다`() {
        val mockRepository = mockk<MemberRepository>()
        val adapter = MemberAdapter(mockRepository)
        every { mockRepository.existsById(1L) } returns true
        every { mockRepository.existsById(2L) } returns false

        adapter.existsById(1L) shouldBe true
        adapter.existsById(2L) shouldBe false
    }

    @Test
    fun `존재하지 않는 provider-providerUserId 조합이면 null을 반환한다`() {
        val adapter = MemberAdapter(memberRepository)

        adapter.findMemberIdByProvider(CoreLoginProvider.KAKAO, "no-such-user") shouldBe null
    }

    @Test
    fun `활성 회원이면 해당 회원의 id를 반환한다`() {
        val adapter = MemberAdapter(memberRepository)
        val saved =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "active-user-1",
                    globalNickname = "닉네임",
                    email = "active1@example.com",
                ),
            )

        adapter.findMemberIdByProvider(CoreLoginProvider.KAKAO, "active-user-1") shouldBe saved.id
    }

    @Test
    fun `탈퇴(소프트 삭제)된 회원은 조회되지 않아 null을 반환한다`() {
        val adapter = MemberAdapter(memberRepository)
        val saved =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "withdrawn-user-1",
                    globalNickname = "탈퇴회원",
                    email = "withdrawn1@example.com",
                ),
            )
        memberRepository.deleteById(saved.id!!)

        adapter.findMemberIdByProvider(CoreLoginProvider.KAKAO, "withdrawn-user-1") shouldBe null
    }
}
