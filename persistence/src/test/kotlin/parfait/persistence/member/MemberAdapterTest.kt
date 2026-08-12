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
import parfait.core.exception.BusinessException
import parfait.core.member.exception.MemberErrorCode
import parfait.core.member.port.out.MemberAccount
import parfait.persistence.TestApplication
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.repository.MemberRepository
import java.util.Optional
import kotlin.test.assertFailsWith
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
    fun `회원이 존재하면 전역 닉네임을 반환한다`() {
        val mockRepository = mockk<MemberRepository>()
        val adapter = MemberAdapter(mockRepository)
        val member =
            Member(
                loginProvider = LoginProvider.KAKAO,
                providerUserId = "provider-id",
                globalNickname = "파르페",
                id = 1L,
            )
        every { mockRepository.findById(1L) } returns Optional.of(member)

        adapter.findGlobalNicknameById(1L) shouldBe "파르페"
    }

    @Test
    fun `회원이 없으면 전역 닉네임 조회 결과는 null이다`() {
        val mockRepository = mockk<MemberRepository>()
        val adapter = MemberAdapter(mockRepository)
        every { mockRepository.findById(1L) } returns Optional.empty()

        adapter.findGlobalNicknameById(1L) shouldBe null
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
                ),
            )
        memberRepository.deleteById(saved.id!!)

        adapter.findMemberIdByProvider(CoreLoginProvider.KAKAO, "withdrawn-user-1") shouldBe null
    }

    @Test
    fun `회원을 생성하면 저장된 회원의 id를 반환하고 조회 가능해진다`() {
        val adapter = MemberAdapter(memberRepository)

        val memberId = adapter.create(CoreLoginProvider.KAKAO, "new-user-1", "말랑한커스터드")

        adapter.findMemberIdByProvider(CoreLoginProvider.KAKAO, "new-user-1") shouldBe memberId
    }

    @Test
    fun `전역 닉네임을 변경하면 저장된 값이 갱신된다`() {
        val adapter = MemberAdapter(memberRepository)
        val saved =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "nickname-update-user-1",
                    globalNickname = "이전 닉네임",
                ),
            )

        adapter.updateGlobalNickname(saved.id!!, "새 닉네임")

        memberRepository.findById(saved.id!!).get().globalNickname shouldBe "새 닉네임"
    }

    @Test
    fun `존재하지 않는 회원의 닉네임을 변경하려 하면 MEMBER_NOT_FOUND 예외를 던진다`() {
        val adapter = MemberAdapter(memberRepository)

        val exception =
            assertFailsWith<BusinessException> {
                adapter.updateGlobalNickname(999_999L, "아무 닉네임")
            }

        exception.errorCode shouldBe MemberErrorCode.MEMBER_NOT_FOUND
    }

    @Test
    fun `탈퇴시키면 provider_user_id가 tombstone 값으로 바뀌고 소프트 삭제된다`() {
        val adapter = MemberAdapter(memberRepository)
        val saved =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "delete-target-user-1",
                    globalNickname = "탈퇴대상회원",
                ),
            )

        adapter.deleteById(saved.id!!)

        memberRepository.findById(saved.id!!).isPresent shouldBe false
        adapter.findMemberIdByProvider(CoreLoginProvider.KAKAO, "delete-target-user-1") shouldBe null
    }

    @Test
    fun `탈퇴 후 동일한 provider_user_id로 재가입할 수 있다`() {
        val adapter = MemberAdapter(memberRepository)
        val saved =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "rejoin-user-1",
                    globalNickname = "재가입전회원",
                ),
            )

        adapter.deleteById(saved.id!!)
        val newMemberId = adapter.create(CoreLoginProvider.KAKAO, "rejoin-user-1", "재가입후닉네임")

        adapter.findMemberIdByProvider(CoreLoginProvider.KAKAO, "rejoin-user-1") shouldBe newMemberId
        (newMemberId == saved.id) shouldBe false
    }

    @Test
    fun `존재하지 않는 회원을 탈퇴시키려 하면 MEMBER_NOT_FOUND 예외를 던진다`() {
        val adapter = MemberAdapter(memberRepository)

        val exception =
            assertFailsWith<BusinessException> {
                adapter.deleteById(999_999L)
            }

        exception.errorCode shouldBe MemberErrorCode.MEMBER_NOT_FOUND
    }

    @Test
    fun `회원이 존재하면 provider와 닉네임을 담은 계정 정보를 반환한다`() {
        val mockRepository = mockk<MemberRepository>()
        val adapter = MemberAdapter(mockRepository)
        val member =
            Member(
                loginProvider = LoginProvider.APPLE,
                providerUserId = "provider-id",
                globalNickname = "파르페",
                id = 1L,
            )
        every { mockRepository.findById(1L) } returns Optional.of(member)

        adapter.findAccountById(1L) shouldBe MemberAccount(CoreLoginProvider.APPLE, "파르페")
    }

    @Test
    fun `회원이 없으면 계정 정보 조회 결과는 null이다`() {
        val mockRepository = mockk<MemberRepository>()
        val adapter = MemberAdapter(mockRepository)
        every { mockRepository.findById(1L) } returns Optional.empty()

        adapter.findAccountById(1L) shouldBe null
    }
}
