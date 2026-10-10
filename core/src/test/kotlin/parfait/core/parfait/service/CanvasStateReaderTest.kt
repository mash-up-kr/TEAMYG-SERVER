package parfait.core.parfait.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.EnsureActiveCanvasUseCase
import parfait.core.parfait.port.out.CanvasCachePort
import parfait.core.parfait.port.out.ParfaitVersionPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupVersionPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class CanvasStateReaderTest {
    private val cache = mockk<CanvasCachePort>(relaxed = true)
    private val parfaitVersionPort = mockk<ParfaitVersionPort>()
    private val groupVersionPort = mockk<ParfaitGroupVersionPort>()
    private val memberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val ensureActiveCanvasUseCase = mockk<EnsureActiveCanvasUseCase>()
    private val reader =
        CanvasStateReader(cache, parfaitVersionPort, groupVersionPort, memberQueryPort, ensureActiveCanvasUseCase)

    private val date = LocalDate.of(2026, 10, 10)

    private fun parfait(id: Long) =
        Parfait.reconstitute(
            id = id,
            parfaitGroupId = 1L,
            parfaitDate = date,
            status = ParfaitStatus.ACTIVE,
            backgroundType = null,
            backgroundValue = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `멤버 캐시가 있으면 DB 를 조회하지 않는다`() {
        every { cache.isMember(1L, 42L) } returns true

        reader.requireMember(1L, 42L)

        verify(exactly = 0) { memberQueryPort.existsByGroupIdAndMemberId(any(), any()) }
    }

    @Test
    fun `멤버 캐시가 없고 DB 에서 멤버로 확인되면 캐시에 저장한다`() {
        every { cache.isMember(1L, 42L) } returns false
        every { memberQueryPort.existsByGroupIdAndMemberId(1L, 42L) } returns true

        reader.requireMember(1L, 42L)

        verify(exactly = 1) { cache.putMember(1L, 42L) }
    }

    @Test
    fun `멤버가 아니면 GROUP_NOT_JOINED 를 던지고 캐시에 저장하지 않는다`() {
        every { cache.isMember(1L, 42L) } returns false
        every { memberQueryPort.existsByGroupIdAndMemberId(1L, 42L) } returns false

        assertFailsWith<ParfaitGroupException> { reader.requireMember(1L, 42L) }
            .error shouldBe ParfaitGroupError.GROUP_NOT_JOINED
        verify(exactly = 0) { cache.putMember(any(), any()) }
    }

    @Test
    fun `오늘 parfaitId 캐시가 있으면 캔버스 확보 로직을 부르지 않는다`() {
        every { cache.getTodayParfaitId(1L, date) } returns 5L
        every { cache.getParfaitVersion(5L) } returns 42L

        reader.todayCanvas(1L, date) shouldBe TodayCanvas(parfaitId = 5L, version = 42L)

        verify(exactly = 0) { ensureActiveCanvasUseCase.ensure(any(), any()) }
    }

    @Test
    fun `오늘 parfaitId 캐시가 없으면 캔버스를 확보하고 캐시에 저장한다`() {
        every { cache.getTodayParfaitId(1L, date) } returns null
        every { ensureActiveCanvasUseCase.ensure(1L, date) } returns parfait(5L)
        every { cache.getParfaitVersion(5L) } returns 42L

        reader.todayCanvas(1L, date) shouldBe TodayCanvas(parfaitId = 5L, version = 42L)

        verify(exactly = 1) { cache.putTodayParfaitId(1L, date, 5L) }
    }

    @Test
    fun `캔버스 version 캐시가 있으면 DB 를 조회하지 않는다`() {
        every { cache.getTodayParfaitId(1L, date) } returns 5L
        every { cache.getParfaitVersion(5L) } returns 42L

        reader.todayCanvas(1L, date).version shouldBe 42L

        verify(exactly = 0) { parfaitVersionPort.getVersion(any()) }
    }

    @Test
    fun `캔버스 version 캐시가 없으면 DB 에서 읽어 캐시에 저장한다`() {
        every { cache.getTodayParfaitId(1L, date) } returns 5L
        every { cache.getParfaitVersion(5L) } returns null
        every { parfaitVersionPort.getVersion(5L) } returns 42L

        reader.todayCanvas(1L, date).version shouldBe 42L

        verify(exactly = 1) { cache.putParfaitVersion(5L, 42L) }
    }

    @Test
    fun `캐시된 오늘 parfaitId 가 DB 에 없으면 캐시를 지우고 캔버스를 다시 확보해 한 번 재조회한다`() {
        every { cache.getTodayParfaitId(1L, date) } returns STALE_ID
        every { cache.getParfaitVersion(any()) } returns null
        every { parfaitVersionPort.getVersion(STALE_ID) } throws BusinessException(ParfaitErrorCode.PARFAIT_NOT_FOUND)
        every { ensureActiveCanvasUseCase.ensure(1L, date) } returns parfait(5L)
        every { parfaitVersionPort.getVersion(5L) } returns 42L

        reader.todayCanvas(1L, date) shouldBe TodayCanvas(parfaitId = 5L, version = 42L)

        verifyOrder {
            parfaitVersionPort.getVersion(STALE_ID)
            cache.evictTodayParfaitId(1L, date)
            ensureActiveCanvasUseCase.ensure(1L, date)
            cache.putTodayParfaitId(1L, date, 5L)
            parfaitVersionPort.getVersion(5L)
        }
        verify(exactly = 1) { ensureActiveCanvasUseCase.ensure(1L, date) }
        verify(exactly = 1) { parfaitVersionPort.getVersion(5L) }
    }

    @Test
    fun `재확보한 캔버스도 DB 에 없으면 한 번만 재시도하고 예외를 던진다`() {
        every { cache.getTodayParfaitId(1L, date) } returns STALE_ID
        every { cache.getParfaitVersion(any()) } returns null
        every { parfaitVersionPort.getVersion(any()) } throws BusinessException(ParfaitErrorCode.PARFAIT_NOT_FOUND)
        every { ensureActiveCanvasUseCase.ensure(1L, date) } returns parfait(5L)

        assertFailsWith<BusinessException> { reader.todayCanvas(1L, date) }
            .errorCode shouldBe ParfaitErrorCode.PARFAIT_NOT_FOUND

        verify(exactly = 1) { parfaitVersionPort.getVersion(STALE_ID) }
        verify(exactly = 1) { parfaitVersionPort.getVersion(5L) }
        verify(exactly = 1) { ensureActiveCanvasUseCase.ensure(1L, date) }
        verify(exactly = 1) { cache.evictTodayParfaitId(1L, date) }
    }

    @Test
    fun `캐시 없이 확보한 캔버스가 DB 에 없으면 복구하지 않고 예외를 던진다`() {
        every { cache.getTodayParfaitId(1L, date) } returns null
        every { cache.getParfaitVersion(any()) } returns null
        every { ensureActiveCanvasUseCase.ensure(1L, date) } returns parfait(5L)
        every { parfaitVersionPort.getVersion(5L) } throws BusinessException(ParfaitErrorCode.PARFAIT_NOT_FOUND)

        assertFailsWith<BusinessException> { reader.todayCanvas(1L, date) }
            .errorCode shouldBe ParfaitErrorCode.PARFAIT_NOT_FOUND

        verify(exactly = 1) { ensureActiveCanvasUseCase.ensure(1L, date) }
        verify(exactly = 1) { parfaitVersionPort.getVersion(5L) }
        verify(exactly = 0) { cache.evictTodayParfaitId(any(), any()) }
    }

    @Test
    fun `PARFAIT_NOT_FOUND 가 아닌 예외는 복구하지 않고 그대로 던진다`() {
        every { cache.getTodayParfaitId(1L, date) } returns 5L
        every { cache.getParfaitVersion(5L) } returns null
        every { parfaitVersionPort.getVersion(5L) } throws IllegalStateException("db down")

        assertFailsWith<IllegalStateException> { reader.todayCanvas(1L, date) }

        verify(exactly = 0) { cache.evictTodayParfaitId(any(), any()) }
        verify(exactly = 0) { ensureActiveCanvasUseCase.ensure(any(), any()) }
    }

    @Test
    fun `PARFAIT_NOT_FOUND 가 아닌 BusinessException 도 복구하지 않고 그대로 던진다`() {
        every { cache.getTodayParfaitId(1L, date) } returns 5L
        every { cache.getParfaitVersion(5L) } returns null
        every { parfaitVersionPort.getVersion(5L) } throws BusinessException(ParfaitErrorCode.PARFAIT_ALREADY_CLOSED)

        assertFailsWith<BusinessException> { reader.todayCanvas(1L, date) }
            .errorCode shouldBe ParfaitErrorCode.PARFAIT_ALREADY_CLOSED

        verify(exactly = 0) { cache.evictTodayParfaitId(any(), any()) }
        verify(exactly = 0) { ensureActiveCanvasUseCase.ensure(any(), any()) }
    }

    @Test
    fun `그룹 version 캐시가 있으면 DB 를 조회하지 않는다`() {
        every { cache.getGroupVersion(1L) } returns 3L

        reader.groupVersion(1L) shouldBe 3L

        verify(exactly = 0) { groupVersionPort.getVersion(any()) }
    }

    @Test
    fun `그룹 version 캐시가 없으면 DB 에서 읽어 캐시에 저장한다`() {
        every { cache.getGroupVersion(1L) } returns null
        every { groupVersionPort.getVersion(1L) } returns 3L

        reader.groupVersion(1L) shouldBe 3L

        verify(exactly = 1) { cache.putGroupVersion(1L, 3L) }
    }

    private companion object {
        const val STALE_ID = 9_999_999L
    }
}
