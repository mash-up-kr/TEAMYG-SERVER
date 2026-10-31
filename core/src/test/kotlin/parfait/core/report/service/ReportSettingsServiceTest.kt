package parfait.core.report.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import parfait.core.report.domain.ReportPlanner
import parfait.core.report.domain.ReportSettings
import parfait.core.report.domain.ReportStatus
import parfait.core.report.port.out.ReportSettingsQueryPort
import parfait.core.report.port.out.ReportSettingsSavePort
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReportSettingsServiceTest {
    private val queryPort = mockk<ReportSettingsQueryPort>()
    private val savePort = mockk<ReportSettingsSavePort>(relaxed = true)
    private val service = ReportSettingsService(queryPort, savePort, ReportPlanner(), LocalTime.of(21, 0))

    private val now = LocalDateTime.of(2026, 10, 30, 20, 0, 0)
    private val today = LocalDate.of(2026, 10, 30)

    @Test
    fun `상태에는 설정값과 다음 리포트 날짜가 담긴다`() {
        val last = LocalDateTime.of(2026, 10, 28, 21, 0, 3)
        every { queryPort.find() } returns ReportSettings(adMode = false, intervalDays = 3, lastSentAt = last)
        val nextDate = LocalDate.of(2026, 10, 31)

        service.getStatus(now) shouldBe
            ReportStatus(adMode = false, intervalDays = 3, lastSentAt = last, nextReportDate = nextDate)
    }

    @Test
    fun `광고모드 상태에서는 어제 전송했으면 오늘이 다음 리포트 날짜다`() {
        val last = LocalDateTime.of(2026, 10, 29, 21, 0, 3)
        every { queryPort.find() } returns ReportSettings(adMode = true, intervalDays = 3, lastSentAt = last)

        service.getStatus(now) shouldBe
            ReportStatus(adMode = true, intervalDays = 3, lastSentAt = last, nextReportDate = today)
    }

    @Test
    fun `전송 시각이 지난 뒤의 상태는 내일을 다음 리포트 날짜로 돌려준다`() {
        every { queryPort.find() } returns ReportSettings(adMode = false, intervalDays = 3, lastSentAt = null)

        service.getStatus(LocalDateTime.of(2026, 10, 30, 22, 0, 0)).nextReportDate shouldBe today.plusDays(1)
    }

    @Test
    fun `광고모드를 켜면 저장한 뒤 다시 읽어 바뀐 상태를 돌려준다`() {
        // 쓰기 전에 읽으면 이전 값(false), 쓰기 뒤에 읽어야 바뀐 값(true)이 나오도록 한다.
        var saved = false
        every { savePort.saveAdMode(true) } answers { saved = true }
        every { queryPort.find() } answers {
            ReportSettings(adMode = if (saved) true else false, intervalDays = 3, lastSentAt = null)
        }

        val status = service.changeAdMode(adMode = true, now = now)

        verifyOrder {
            savePort.saveAdMode(true)
            queryPort.find()
        }
        status shouldBe ReportStatus(adMode = true, intervalDays = 3, lastSentAt = null, nextReportDate = today)
    }

    @Test
    fun `광고모드를 끄면 저장하고 바뀐 상태를 돌려준다`() {
        // 쓰기 전에 읽으면 이전 값(true), 쓰기 뒤에 읽어야 바뀐 값(false)이 나오도록 한다.
        var saved = false
        every { savePort.saveAdMode(false) } answers { saved = true }
        every { queryPort.find() } answers {
            ReportSettings(adMode = if (saved) false else true, intervalDays = 3, lastSentAt = null)
        }

        val status = service.changeAdMode(adMode = false, now = now)

        verifyOrder {
            savePort.saveAdMode(false)
            queryPort.find()
        }
        status.adMode shouldBe false
    }

    @Test
    fun `이미 켜진 광고모드를 다시 켜도 같은 상태를 돌려준다`() {
        every { queryPort.find() } returns ReportSettings(adMode = true, intervalDays = 3, lastSentAt = null)

        val first = service.changeAdMode(adMode = true, now = now)
        val second = service.changeAdMode(adMode = true, now = now)

        first shouldBe second
        verify(exactly = 2) { savePort.saveAdMode(true) }
    }
}
