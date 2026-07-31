package parfait.persistence.adapter

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.persistence.repository.MemberRepository

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
}
