package parfait.core.parfait.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.GetPastParfaitsCommand
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSummary
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import java.time.LocalDate
import kotlin.test.assertFailsWith

class GetPastParfaitsServiceTest {
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val parfaitImageQueryPort = mockk<ParfaitImageQueryPort>()
    private val service =
        GetPastParfaitsService(parfaitGroupMemberQueryPort, parfaitQueryPort, parfaitImageQueryPort)

    @Test
    fun `명시적 범위로 조회하면 최신순 목록과 토핑 수를 매핑해 반환한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        val from = LocalDate.of(2026, 7, 1)
        val to = LocalDate.of(2026, 7, 31)
        every { parfaitQueryPort.findAllByGroupIdAndDateRange(1L, from, to) } returns
            listOf(
                ParfaitSummary(id = 98L, date = LocalDate.of(2026, 7, 7)),
                ParfaitSummary(id = 97L, date = LocalDate.of(2026, 7, 6)),
            )
        every { parfaitImageQueryPort.countAllByParfaitIds(listOf(98L, 97L)) } returns mapOf(98L to 4)

        val result =
            service.getPastParfaits(
                GetPastParfaitsCommand(memberId = 10L, groupId = 1L, from = from, to = to),
            )

        result.size shouldBe 2
        result[0].parfaitId shouldBe 98L
        result[0].imageCount shouldBe 4
        result[0].thumbnailUrl shouldBe null
        result[1].parfaitId shouldBe 97L
        result[1].imageCount shouldBe 0
    }

    @Test
    fun `from과 to를 생략하면 오늘부터 30일 전까지 기본 범위로 조회한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        val today = LocalDate.now()
        every {
            parfaitQueryPort.findAllByGroupIdAndDateRange(1L, today.minusDays(30), today)
        } returns emptyList()

        service.getPastParfaits(GetPastParfaitsCommand(memberId = 10L, groupId = 1L, from = null, to = null))

        verify { parfaitQueryPort.findAllByGroupIdAndDateRange(1L, today.minusDays(30), today) }
    }

    @Test
    fun `from만 생략하면 to의 30일 전을 시작일로 사용한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        val to = LocalDate.of(2026, 7, 31)
        every {
            parfaitQueryPort.findAllByGroupIdAndDateRange(1L, to.minusDays(30), to)
        } returns emptyList()

        service.getPastParfaits(GetPastParfaitsCommand(memberId = 10L, groupId = 1L, from = null, to = to))

        verify { parfaitQueryPort.findAllByGroupIdAndDateRange(1L, to.minusDays(30), to) }
    }

    @Test
    fun `조회 시작일이 종료일보다 늦으면 INVALID_DATE_RANGE를 던진다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true

        val exception =
            assertFailsWith<BusinessException> {
                service.getPastParfaits(
                    GetPastParfaitsCommand(
                        memberId = 10L,
                        groupId = 1L,
                        from = LocalDate.of(2026, 8, 1),
                        to = LocalDate.of(2026, 7, 1),
                    ),
                )
            }
        exception.errorCode shouldBe ParfaitErrorCode.INVALID_DATE_RANGE
    }

    @Test
    fun `조회된 파르페가 없으면 빈 목록을 반환하고 토핑 수 조회는 건너뛴다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findAllByGroupIdAndDateRange(1L, any(), any()) } returns emptyList()

        val result =
            service.getPastParfaits(
                GetPastParfaitsCommand(memberId = 10L, groupId = 1L, from = null, to = null),
            )

        result shouldBe emptyList()
        verify(exactly = 0) { parfaitImageQueryPort.countAllByParfaitIds(any()) }
    }

    @Test
    fun `그룹에 참여하지 않은 회원은 조회를 거부한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns false

        val exception =
            assertFailsWith<ParfaitGroupException> {
                service.getPastParfaits(GetPastParfaitsCommand(memberId = 10L, groupId = 1L, from = null, to = null))
            }
        exception.error shouldBe ParfaitGroupError.GROUP_NOT_JOINED
    }
}
