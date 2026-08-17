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

    @Test
    fun `changeBackground는 COLOR 타입에 유효한 HEX 값을 설정한다`() {
        val parfait = Parfait.createToday(parfaitGroupId = 1L, date = date, now = now)
        val changedAt = now.plusHours(1)

        val changed = parfait.changeBackground(BackgroundType.COLOR, "#FF5733", changedAt)

        changed.backgroundType shouldBe BackgroundType.COLOR
        changed.backgroundValue shouldBe "#FF5733"
        changed.updatedAt shouldBe changedAt
    }

    @Test
    fun `changeBackground는 IMAGE 타입에 URL 값을 그대로 설정한다`() {
        val parfait = Parfait.createToday(parfaitGroupId = 1L, date = date, now = now)

        val changed = parfait.changeBackground(BackgroundType.IMAGE, "https://s3.example/background.png", now)

        changed.backgroundType shouldBe BackgroundType.IMAGE
        changed.backgroundValue shouldBe "https://s3.example/background.png"
    }

    @Test
    fun `changeBackground는 COLOR 타입에 잘못된 HEX 형식이면 INVALID_BACKGROUND를 던진다`() {
        val parfait = Parfait.createToday(parfaitGroupId = 1L, date = date, now = now)

        val exception =
            assertFailsWith<BusinessException> {
                parfait.changeBackground(BackgroundType.COLOR, "FF5733", now)
            }
        exception.errorCode shouldBe ParfaitErrorCode.INVALID_BACKGROUND
    }
}
