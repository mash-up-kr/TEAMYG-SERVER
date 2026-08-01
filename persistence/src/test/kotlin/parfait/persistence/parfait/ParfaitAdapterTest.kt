package parfait.persistence.parfait

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.persistence.repository.ParfaitRepository

class ParfaitAdapterTest {
    private val parfaitRepository = mockk<ParfaitRepository>()
    private val adapter = ParfaitAdapter(parfaitRepository)

    @Test
    fun `그룹 id로 토핑이 존재하는 연도 목록 조회를 리포지토리에 위임한다`() {
        every { parfaitRepository.findDistinctYearsByParfaitGroupId(1L) } returns listOf(2026, 2027)

        val result = adapter.findDistinctYearsByGroupId(1L)

        result shouldBe listOf(2026, 2027)
    }
}
