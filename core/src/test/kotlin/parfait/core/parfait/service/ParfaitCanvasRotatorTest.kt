package parfait.core.parfait.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSavePort
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import java.time.LocalDate
import java.time.LocalDateTime

class ParfaitCanvasRotatorTest {
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val parfaitImageQueryPort = mockk<ParfaitImageQueryPort>()
    private val parfaitSavePort = mockk<ParfaitSavePort>()
    private val rotator = ParfaitCanvasRotator(parfaitQueryPort, parfaitImageQueryPort, parfaitSavePort)
    private val today = LocalDate.of(2026, 8, 13)

    private fun activeParfait(
        id: Long,
        date: LocalDate = today,
    ): Parfait =
        Parfait.reconstitute(
            id = id,
            parfaitGroupId = 1L,
            parfaitDate = date,
            status = ParfaitStatus.ACTIVE,
            backgroundType = null,
            backgroundValue = null,
            createdAt = date.atStartOfDay(),
            updatedAt = date.atStartOfDay(),
        )

    @Test
    fun `ACTIVE 캔버스가 없으면 아무 것도 하지 않고 null을 반환한다`() {
        every { parfaitQueryPort.findActiveByGroupId(1L) } returns null

        rotator.rotateOne(1L) shouldBe null
        verify(exactly = 0) { parfaitSavePort.save(any()) }
    }

    @Test
    fun `ACTIVE 캔버스 날짜가 미래면 건드리지 않고 null을 반환한다`() {
        // today(2026-08-13)는 고정값이라 테스트 실행 시점의 실제 오늘 날짜(LocalDate.now())를 알 수 없으므로,
        // "미래"임이 항상 보장되도록 실제 현재 날짜 기준으로 충분히 먼 미래 날짜를 사용한다.
        every {
            parfaitQueryPort.findActiveByGroupId(1L)
        } returns activeParfait(1L, date = LocalDate.now().plusYears(1))

        rotator.rotateOne(1L) shouldBe null
        verify(exactly = 0) { parfaitSavePort.save(any()) }
    }

    @Test
    fun `ACTIVE 캔버스 날짜가 오늘이면 이미 회전된 것으로 보고 건드리지 않는다`() {
        // 자정~새벽 3시 사이 ensure()로 "오늘" 캔버스가 이미 활성 상태로 만들어진 뒤,
        // 같은 날 배치가 다시 rotateOne을 실행하는 상황을 재현한다.
        every { parfaitQueryPort.findActiveByGroupId(1L) } returns activeParfait(1L, date = LocalDate.now())

        rotator.rotateOne(1L) shouldBe null
        verify(exactly = 0) { parfaitSavePort.save(any()) }
    }

    @Test
    fun `새벽 3시 이전에 호출되면 ParfaitDay 기준 전날인 ACTIVE 캔버스도 건드리지 않는다`() {
        // 테스트 트리거 엔드포인트처럼 새벽 3시 이전에 rotateOne이 호출되는 상황을 재현한다.
        // 캘린더로는 이미 8/17이지만 ParfaitDay 기준으로는 아직 8/16이 안 끝났으므로,
        // 8/16 날짜인 ACTIVE 캔버스를 마감하면 안 된다.
        val now = LocalDateTime.of(2026, 8, 17, 1, 0, 0)
        every {
            parfaitQueryPort.findActiveByGroupId(1L)
        } returns activeParfait(1L, date = LocalDate.of(2026, 8, 16))

        rotator.rotateOne(1L, now) shouldBe null
        verify(exactly = 0) { parfaitSavePort.save(any()) }
    }

    @Test
    fun `새벽 3시 이후에 호출되면 ParfaitDay 기준 전날인 ACTIVE 캔버스를 마감하고 새 캔버스를 만든다`() {
        val now = LocalDateTime.of(2026, 8, 17, 3, 0, 1)
        every {
            parfaitQueryPort.findActiveByGroupId(1L)
        } returns activeParfait(1L, date = LocalDate.of(2026, 8, 16))
        every { parfaitImageQueryPort.existsByParfaitId(1L) } returns false
        every { parfaitSavePort.save(any()) } answers { firstArg() }
        every { parfaitQueryPort.findByGroupIdAndDate(1L, LocalDate.of(2026, 8, 17)) } returns null

        val result = rotator.rotateOne(1L, now)!!

        result.created shouldBe true
        verify {
            parfaitSavePort.save(
                match { it.status == ParfaitStatus.ACTIVE && it.parfaitDate == LocalDate.of(2026, 8, 17) },
            )
        }
    }

    @Test
    fun `낮에 미리 회전시켜 만든 내일 캔버스는 다음날 새벽 3시 배치에서 또 밀리지 않는다`() {
        // 테스트 트리거 엔드포인트로 8/17 낮에 미리 회전시켰다고 가정하면 8/18 캔버스가
        // ACTIVE로 만들어진다. 그 상태에서 실제로 하루가 지나 8/18 새벽 3시 배치가 돌아도,
        // 이 캔버스는 ParfaitDay 기준 "오늘"이지 "지난 영업일"이 아니므로 또 마감되면 안 된다
        // — isAfter 기준이었다면 여기서 또 통과해 8/19로 영구히 밀려나갔을 것이다.
        val nextDayBatchTime = LocalDateTime.of(2026, 8, 18, 3, 0, 1)
        every {
            parfaitQueryPort.findActiveByGroupId(1L)
        } returns activeParfait(1L, date = LocalDate.of(2026, 8, 18))

        rotator.rotateOne(1L, nextDayBatchTime) shouldBe null
        verify(exactly = 0) { parfaitSavePort.save(any()) }
    }

    @Test
    fun `forceRotateOne은 오늘 날짜인 ACTIVE 캔버스도 가드 없이 즉시 마감하고 다음날 캔버스를 만든다`() {
        // 테스트 트리거 엔드포인트 전용 경로. ParfaitDay 가드를 건너뛴다는 점만 rotateOne과 다르다.
        val now = LocalDateTime.of(2026, 8, 17, 14, 0, 0)
        every {
            parfaitQueryPort.findActiveByGroupId(1L)
        } returns activeParfait(1L, date = LocalDate.of(2026, 8, 17))
        every { parfaitImageQueryPort.existsByParfaitId(1L) } returns false
        every { parfaitSavePort.save(any()) } answers { firstArg() }
        every { parfaitQueryPort.findByGroupIdAndDate(1L, LocalDate.of(2026, 8, 18)) } returns null

        val result = rotator.forceRotateOne(1L, now)!!

        result.wasEmpty shouldBe true
        result.created shouldBe true
        verify {
            parfaitSavePort.save(
                match { it.status == ParfaitStatus.ACTIVE && it.parfaitDate == LocalDate.of(2026, 8, 18) },
            )
        }
    }

    @Test
    fun `forceRotateOne도 다음 날짜 캔버스가 이미 있으면 생성을 생략한다`() {
        every { parfaitQueryPort.findActiveByGroupId(1L) } returns activeParfait(3L)
        every { parfaitImageQueryPort.existsByParfaitId(3L) } returns true
        every { parfaitSavePort.save(any()) } answers { firstArg() }
        every {
            parfaitQueryPort.findByGroupIdAndDate(1L, today.plusDays(1))
        } returns Parfait.createToday(parfaitGroupId = 1L, date = today.plusDays(1))

        val result = rotator.forceRotateOne(1L)!!

        result.created shouldBe false
        verify(exactly = 1) { parfaitSavePort.save(any()) }
    }

    @Test
    fun `토핑이 있으면 CLOSED로 마감하고 다음날 캔버스를 새로 생성한다`() {
        every { parfaitQueryPort.findActiveByGroupId(1L) } returns activeParfait(1L)
        every { parfaitImageQueryPort.existsByParfaitId(1L) } returns true
        every { parfaitSavePort.save(any()) } answers { firstArg() }
        every { parfaitQueryPort.findByGroupIdAndDate(1L, today.plusDays(1)) } returns null

        val result = rotator.rotateOne(1L)!!

        result.wasEmpty shouldBe false
        result.created shouldBe true
        verify { parfaitSavePort.save(match { it.status == ParfaitStatus.CLOSED }) }
        verify {
            parfaitSavePort.save(match { it.status == ParfaitStatus.ACTIVE && it.parfaitDate == today.plusDays(1) })
        }
    }

    @Test
    fun `토핑이 없으면 EMPTY로 마감한다`() {
        every { parfaitQueryPort.findActiveByGroupId(1L) } returns activeParfait(2L)
        every { parfaitImageQueryPort.existsByParfaitId(2L) } returns false
        every { parfaitSavePort.save(any()) } answers { firstArg() }
        every { parfaitQueryPort.findByGroupIdAndDate(1L, today.plusDays(1)) } returns null

        val result = rotator.rotateOne(1L)!!

        result.wasEmpty shouldBe true
        verify { parfaitSavePort.save(match { it.status == ParfaitStatus.EMPTY }) }
    }

    @Test
    fun `다음 날짜 캔버스가 이미 있으면 생성을 생략한다`() {
        every { parfaitQueryPort.findActiveByGroupId(1L) } returns activeParfait(3L)
        every { parfaitImageQueryPort.existsByParfaitId(3L) } returns true
        every { parfaitSavePort.save(any()) } answers { firstArg() }
        every {
            parfaitQueryPort.findByGroupIdAndDate(1L, today.plusDays(1))
        } returns Parfait.createToday(parfaitGroupId = 1L, date = today.plusDays(1))

        val result = rotator.rotateOne(1L)!!

        result.created shouldBe false
        verify(exactly = 1) { parfaitSavePort.save(any()) }
    }
}
