package parfait.persistence.adapter

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.repository.MemberRepository
import java.util.Optional

class MemberAdapterTest {
    private val memberRepository = mockk<MemberRepository>()
    private val adapter = MemberAdapter(memberRepository)

    @Test
    fun `memberRepository의 existsById 결과를 그대로 반환한다`() {
        every { memberRepository.existsById(1L) } returns true
        every { memberRepository.existsById(2L) } returns false

        adapter.existsById(1L) shouldBe true
        adapter.existsById(2L) shouldBe false
    }

    @Test
    fun `회원이 존재하면 전역 닉네임을 반환한다`() {
        val member =
            Member(
                loginProvider = LoginProvider.KAKAO,
                providerUserId = "provider-id",
                globalNickname = "파르페",
                email = "member@example.com",
                id = 1L,
            )
        every { memberRepository.findById(1L) } returns Optional.of(member)

        adapter.findGlobalNicknameById(1L) shouldBe "파르페"
    }

    @Test
    fun `회원이 없으면 전역 닉네임 조회 결과는 null이다`() {
        every { memberRepository.findById(1L) } returns Optional.empty()

        adapter.findGlobalNicknameById(1L) shouldBe null
    }
}
