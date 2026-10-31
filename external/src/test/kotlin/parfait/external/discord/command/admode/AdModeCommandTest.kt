package parfait.external.discord.command.admode

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.report.domain.ReportStatus
import parfait.core.report.port.`in`.ChangeAdModeUseCase
import parfait.core.report.port.`in`.GetReportStatusUseCase
import parfait.external.discord.command.CommandRequest
import parfait.external.discord.command.fixedClock
import java.time.LocalDate
import java.time.LocalDateTime

class AdModeCommandTest {
    private val changeAdMode = mockk<ChangeAdModeUseCase>()
    private val getStatus = mockk<GetReportStatusUseCase>()
    private val now = LocalDateTime.of(2026, 10, 30, 21, 0)
    private val command = AdModeCommand(changeAdMode, getStatus, fixedClock(now))

    private fun request(subcommand: String?) =
        CommandRequest(subcommand = subcommand, options = emptyMap(), isAdmin = true)

    @Test
    fun `켜기는 광고모드를 켜고 채널에 공개로 알린다`() {
        every { changeAdMode.changeAdMode(true, now) } returns
            ReportStatus(
                adMode = true,
                intervalDays = 3,
                lastSentAt = null,
                nextReportDate = LocalDate.of(2026, 10, 30),
            )

        val reply = command.execute(request("켜기"))

        reply.text shouldBe "광고모드를 켰습니다. 광고모드: ON · 주기: 매일 · 마지막 전송: 없음 · 다음 리포트: 10/30"
        reply.ephemeral shouldBe false
        verify(exactly = 1) { changeAdMode.changeAdMode(true, now) }
    }

    @Test
    fun `끄기는 광고모드를 끄고 채널에 공개로 알린다`() {
        val last = LocalDateTime.of(2026, 10, 29, 21, 0, 3)
        every { changeAdMode.changeAdMode(false, now) } returns
            ReportStatus(
                adMode = false,
                intervalDays = 3,
                lastSentAt = last,
                nextReportDate = LocalDate.of(2026, 11, 1),
            )

        val reply = command.execute(request("끄기"))

        reply.text shouldBe "광고모드를 껐습니다. 광고모드: OFF · 주기: 3일마다 · 마지막 전송: 10/29 21:00 · 다음 리포트: 11/1"
        reply.ephemeral shouldBe false
    }

    @Test
    fun `상태는 설정을 바꾸지 않고 요청자에게만 보여준다`() {
        every { getStatus.getStatus(now) } returns
            ReportStatus(
                adMode = false,
                intervalDays = 3,
                lastSentAt = null,
                nextReportDate = LocalDate.of(2026, 10, 30),
            )

        val reply = command.execute(request("상태"))

        reply.text shouldBe "광고모드: OFF · 주기: 3일마다 · 마지막 전송: 없음 · 다음 리포트: 10/30"
        reply.ephemeral shouldBe true
        verify(exactly = 0) { changeAdMode.changeAdMode(any(), any()) }
    }

    @Test
    fun `모르는 하위 명령어면 안내한다`() {
        val reply = command.execute(request("뭐지"))

        reply.text shouldBe "켜기, 끄기, 상태 중에서 골라 주세요."
        reply.ephemeral shouldBe true
    }
}
