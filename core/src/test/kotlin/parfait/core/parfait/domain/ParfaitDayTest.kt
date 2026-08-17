package parfait.core.parfait.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ParfaitDayTest {
    @Test
    fun `자정 직후면 전날 날짜를 반환한다`() {
        val now = LocalDateTime.of(2026, 8, 17, 0, 14, 57)

        ParfaitDay.current(now) shouldBe LocalDate.of(2026, 8, 16)
    }

    @Test
    fun `새벽 2시 59분 59초까지는 전날 날짜를 반환한다`() {
        val now = LocalDateTime.of(2026, 8, 17, 2, 59, 59)

        ParfaitDay.current(now) shouldBe LocalDate.of(2026, 8, 16)
    }

    @Test
    fun `새벽 3시 정각부터는 당일 날짜를 반환한다`() {
        val now = LocalDateTime.of(2026, 8, 17, 3, 0, 0)

        ParfaitDay.current(now) shouldBe LocalDate.of(2026, 8, 17)
    }

    @Test
    fun `오후 시간대는 당일 날짜를 반환한다`() {
        val now = LocalDateTime.of(2026, 8, 17, 17, 53, 40)

        ParfaitDay.current(now) shouldBe LocalDate.of(2026, 8, 17)
    }
}
