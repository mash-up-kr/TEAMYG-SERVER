package parfait.core.parfait.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.port.`in`.GetTodayParfaitCommand
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedCommand
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedResult
import parfait.core.parfait.port.`in`.GetTodayParfaitResult
import parfait.core.parfait.port.`in`.GetTodayParfaitUseCase
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import java.time.LocalDate
import kotlin.test.assertFailsWith

class GetTodayParfaitIfChangedServiceTest {
    private val reader = mockk<CanvasStateReader>()
    private val getTodayParfaitUseCase = mockk<GetTodayParfaitUseCase>()
    private val service = GetTodayParfaitIfChangedService(reader, getTodayParfaitUseCase)

    private val body =
        GetTodayParfaitResult(
            parfaitId = 5L,
            groupName = "파르페",
            date = LocalDate.of(2026, 10, 10),
            status = ParfaitStatus.ACTIVE,
            lastClosedDate = null,
            groupMembers = emptyList(),
            background = null,
            images = null,
        )

    private val currentEtag = "W/\"p5-v3-g2-m42\""

    private fun stubState() {
        every { reader.requireMember(1L, 42L) } returns Unit
        every { reader.todayCanvas(1L, any()) } returns TodayCanvas(parfaitId = 5L, version = 3L)
        every { reader.groupVersion(1L) } returns 2L
    }

    private fun command(ifNoneMatch: String?) =
        GetTodayParfaitIfChangedCommand(
            memberId = 42L,
            groupId = 1L,
            ifNoneMatch = ifNoneMatch,
        )

    @Test
    fun `If-None-Match 가 없으면 본문을 만들어 200 용 결과를 돌려준다`() {
        stubState()
        every { getTodayParfaitUseCase.get(GetTodayParfaitCommand(memberId = 42L, groupId = 1L)) } returns body

        val result = service.get(command(null))

        result shouldBe GetTodayParfaitIfChangedResult.Modified(currentEtag, body)
    }

    @Test
    fun `If-None-Match 가 현재 ETag 와 같으면 본문을 만들지 않고 NotModified 를 돌려준다`() {
        stubState()

        val result = service.get(command(currentEtag))

        result shouldBe GetTodayParfaitIfChangedResult.NotModified(currentEtag)
        verify(exactly = 0) { getTodayParfaitUseCase.get(any()) }
    }

    @Test
    fun `이전 ETag 면 새 본문과 새 ETag 를 돌려준다`() {
        stubState()
        every { getTodayParfaitUseCase.get(any()) } returns body

        val result = service.get(command("W/\"p5-v2-g2-m42\""))

        result shouldBe GetTodayParfaitIfChangedResult.Modified(currentEtag, body)
    }

    @Test
    fun `version 은 본문을 만들기 전에 읽는다`() {
        stubState()
        every { getTodayParfaitUseCase.get(any()) } returns body

        service.get(command(null))

        verifyOrder {
            reader.todayCanvas(1L, any())
            reader.groupVersion(1L)
            getTodayParfaitUseCase.get(any())
        }
    }

    @Test
    fun `그룹 멤버가 아니면 거부하고 ETag 계산도 하지 않는다`() {
        every { reader.requireMember(1L, 42L) } throws ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

        assertFailsWith<ParfaitGroupException> { service.get(command(currentEtag)) }
            .error shouldBe ParfaitGroupError.GROUP_NOT_JOINED
        verify(exactly = 0) { reader.todayCanvas(any(), any()) }
        verify(exactly = 0) { reader.groupVersion(any()) }
        verify(exactly = 0) { getTodayParfaitUseCase.get(any()) }
    }
}
