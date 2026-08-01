package parfait.persistence.adapter

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.parfaitgroup.domain.ParfaitGroupReport
import parfait.persistence.entity.ParfaitGroupReporting
import parfait.persistence.repository.ParfaitGroupReportingRepository
import java.time.LocalDateTime

class ParfaitGroupReportingAdapterTest {
    private val reportingRepository = mockk<ParfaitGroupReportingRepository>()
    private val adapter = ParfaitGroupReportingAdapter(reportingRepository)

    @Test
    fun `도메인 신고를 엔티티로 저장하고 저장된 신고를 도메인으로 반환한다`() {
        val now = LocalDateTime.of(2026, 8, 1, 12, 0)
        val report = ParfaitGroupReport.create(1L, 10L, "신고 사유", now)
        every { reportingRepository.save(any()) } answers {
            firstArg<ParfaitGroupReporting>().apply { id = 99L }
        }

        val saved = adapter.save(report)

        saved.id shouldBe 99L
        saved.parfaitGroupId shouldBe 1L
        saved.reporterMemberId shouldBe 10L
        saved.reason shouldBe "신고 사유"
    }
}
