package parfait.persistence.parfait

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.parfait.port.out.ParfaitSummary
import parfait.persistence.repository.ParfaitRepository
import java.time.LocalDate
import java.time.LocalDateTime
import parfait.persistence.entity.Parfait as ParfaitEntity

class ParfaitAdapterTest {
    private val parfaitRepository = mockk<ParfaitRepository>()
    private val adapter = ParfaitAdapter(parfaitRepository)

    @Test
    fun `그룹 id로 토핑이 존재하는 연도 목록 조회를 리포지토리에 위임한다`() {
        every { parfaitRepository.findDistinctYearsByParfaitGroupId(1L) } returns listOf(2026, 2027)

        val result = adapter.findDistinctYearsByGroupId(1L)

        result shouldBe listOf(2026, 2027)
    }

    @Test
    fun `날짜 범위로 조회한 JPA 엔티티 목록을 ParfaitSummary로 변환한다`() {
        val from = LocalDate.of(2026, 7, 1)
        val to = LocalDate.of(2026, 7, 31)
        val now = LocalDateTime.of(2026, 7, 7, 12, 0)
        every {
            parfaitRepository.findAllByParfaitGroupIdAndParfaitDateBetweenOrderByParfaitDateDesc(
                1L,
                from,
                to,
            )
        } returns
            listOf(
                ParfaitEntity(
                    parfaitGroupId = 1L,
                    parfaitDate = LocalDate.of(2026, 7, 7),
                    createdAt = now,
                    updatedAt = now,
                    id = 98L,
                ),
            )

        val result = adapter.findAllByGroupIdAndDateRange(1L, from, to)

        result shouldBe listOf(ParfaitSummary(id = 98L, date = LocalDate.of(2026, 7, 7)))
    }
}
