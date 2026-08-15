package parfait.core.parfait.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.retry.support.RetryTemplate
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import java.time.LocalDate
import kotlin.test.assertFailsWith

class ParfaitServiceTest {
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitSavePort = mockk<ParfaitSavePort>()
    private val parfaitGroupQueryPort = mockk<ParfaitGroupQueryPort>()
    private val parfaitCanvasRotator = mockk<ParfaitCanvasRotator>()
    private val retryTemplate = RetryTemplate.builder().maxAttempts(1).build()
    private val service =
        ParfaitService(
            parfaitQueryPort,
            parfaitGroupMemberQueryPort,
            parfaitSavePort,
            parfaitGroupQueryPort,
            parfaitCanvasRotator,
            retryTemplate,
        )

    @Test
    fun `그룹 멤버는 토핑이 업로드된 연도 목록을 오름차순으로 조회한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findDistinctYearsByGroupId(1L) } returns listOf(2026, 2027)

        val result = service.getYears(memberId = 10L, groupId = 1L)

        result shouldBe listOf(2026, 2027)
    }

    @Test
    fun `그룹에 참여하지 않은 회원은 조회를 거부한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns false

        val exception =
            assertFailsWith<ParfaitGroupException> {
                service.getYears(memberId = 10L, groupId = 1L)
            }
        exception.error shouldBe ParfaitGroupError.GROUP_NOT_JOINED
    }

    @Test
    fun `대상 날짜에 캔버스가 없으면 새로 생성해 저장한다`() {
        val date = LocalDate.of(2026, 8, 14)
        every { parfaitQueryPort.findByGroupIdAndDate(1L, date) } returns null
        every { parfaitSavePort.save(any()) } answers { firstArg() }

        val result = service.ensure(groupId = 1L, targetDate = date)

        result.parfaitGroupId shouldBe 1L
        result.parfaitDate shouldBe date
        verify(exactly = 1) { parfaitSavePort.save(any()) }
    }

    @Test
    fun `대상 날짜에 이미 캔버스가 있으면 그대로 반환하고 저장하지 않는다`() {
        val date = LocalDate.of(2026, 8, 14)
        val existing = Parfait.createToday(parfaitGroupId = 1L, date = date)
        every { parfaitQueryPort.findByGroupIdAndDate(1L, date) } returns existing

        val result = service.ensure(groupId = 1L, targetDate = date)

        result shouldBe existing
        verify(exactly = 0) { parfaitSavePort.save(any()) }
    }

    @Test
    fun `그룹별 마감·생성 결과를 합산해 반환한다`() {
        every { parfaitGroupQueryPort.findAllIds() } returns listOf(1L, 2L, 3L)
        every { parfaitCanvasRotator.rotateOne(1L) } returns RotateOneResult(wasEmpty = false, created = true)
        every { parfaitCanvasRotator.rotateOne(2L) } returns RotateOneResult(wasEmpty = true, created = true)
        every { parfaitCanvasRotator.rotateOne(3L) } returns RotateOneResult(wasEmpty = true, created = true)

        val result = service.rotateAll()

        result.closedCount shouldBe 1
        result.emptyCount shouldBe 2
        result.createdCount shouldBe 3
        result.failedCount shouldBe 0
    }

    @Test
    fun `한 그룹이 재시도를 소진해도 예외를 전파하지 않고 다음 그룹을 계속 처리한다`() {
        every { parfaitGroupQueryPort.findAllIds() } returns listOf(1L, 2L)
        every { parfaitCanvasRotator.rotateOne(1L) } throws RuntimeException("일시적 DB 오류")
        every { parfaitCanvasRotator.rotateOne(2L) } returns RotateOneResult(wasEmpty = false, created = true)

        val result = service.rotateAll()

        result.closedCount shouldBe 1
        result.createdCount shouldBe 1
        result.failedCount shouldBe 1
    }
}
