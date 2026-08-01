package parfait.core.parfait.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import kotlin.test.assertFailsWith

class ParfaitServiceTest {
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val service = ParfaitService(parfaitQueryPort, parfaitGroupMemberQueryPort)

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
}
