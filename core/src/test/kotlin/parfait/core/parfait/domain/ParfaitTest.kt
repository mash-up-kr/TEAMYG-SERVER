package parfait.core.parfait.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.parfait.exception.ParfaitErrorCode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class ParfaitTest {
    private val date = LocalDate.of(2026, 8, 13)
    private val now = LocalDateTime.of(2026, 8, 13, 3, 0)

    @Test
    fun `close는 ACTIVE를 CLOSED로 전환하고 updatedAt을 갱신한다`() {
        val parfait = Parfait.createToday(parfaitGroupId = 1L, date = date, now = now)
        val closedAt = now.plusHours(1)

        val closed = parfait.close(closedAt)

        closed.status shouldBe ParfaitStatus.CLOSED
        closed.updatedAt shouldBe closedAt
        closed.createdAt shouldBe now
    }

    @Test
    fun `markEmpty는 ACTIVE를 EMPTY로 전환한다`() {
        val parfait = Parfait.createToday(parfaitGroupId = 1L, date = date, now = now)
        val emptyAt = now.plusHours(1)

        val empty = parfait.markEmpty(emptyAt)

        empty.status shouldBe ParfaitStatus.EMPTY
        empty.updatedAt shouldBe emptyAt
        empty.createdAt shouldBe now
    }

    @Test
    fun `ACTIVE가 아닌 파르페는 다시 마감할 수 없다`() {
        val closed = Parfait.createToday(parfaitGroupId = 1L, date = date, now = now).close(now)

        val closeException = assertFailsWith<BusinessException> { closed.close(now) }
        closeException.errorCode shouldBe ParfaitErrorCode.PARFAIT_ALREADY_CLOSED

        val markEmptyException = assertFailsWith<BusinessException> { closed.markEmpty(now) }
        markEmptyException.errorCode shouldBe ParfaitErrorCode.PARFAIT_ALREADY_CLOSED
    }
}
