package parfait.core.notification.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.notification.domain.NotificationOutbox
import parfait.core.notification.domain.NotificationSettings
import parfait.core.notification.domain.OutboxStatus
import parfait.core.notification.domain.ToppingPlacedPayload
import parfait.core.notification.port.out.NotificationOutboxAppendPort
import parfait.core.notification.port.out.NotificationOutboxDigestPort
import parfait.core.notification.port.out.NotificationSettingsQueryPort
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaittext.domain.ParfaitText
import parfait.core.parfaittext.port.out.ParfaitTextQueryPort
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityDigestCoordinatorTest {
    private val settingsQueryPort = mockk<NotificationSettingsQueryPort>()
    private val digestPort = mockk<NotificationOutboxDigestPort>(relaxed = true)
    private val appendPort = mockk<NotificationOutboxAppendPort>(relaxed = true)
    private val imageQueryPort = mockk<ParfaitImageQueryPort>()
    private val textQueryPort = mockk<ParfaitTextQueryPort>()
    private val coordinator =
        ActivityDigestCoordinator(settingsQueryPort, digestPort, appendPort, imageQueryPort, textQueryPort)

    private val now = LocalDateTime.of(2026, 11, 3, 10, 0, 0)
    private val date = LocalDate.of(2026, 11, 3)
    private val payload = ToppingPlacedPayload(groupId = 1L, parfaitId = 5L, parfaitDate = date, actorMemberId = 7L)

    private fun row(
        id: Long,
        eventType: String = "TOPPING_PLACED",
        aggregateId: Long = id,
    ) = NotificationOutbox.reconstitute(
        id = id,
        aggregateType = "TOPPING",
        aggregateId = aggregateId,
        eventType = eventType,
        receiverMemberId = 42L,
        payload = payload,
        dedupKey = "k$id",
        status = OutboxStatus.PENDING,
        attempts = 0,
        scheduledAt = now,
        lastError = null,
        createdAt = now,
        sentAt = null,
    )

    @Test
    fun `모아 받기가 꺼져 있으면 바로 보내고 윈도우를 열지 않는다`() {
        every { settingsQueryPort.findByMemberId(42L) } returns NotificationSettings.DEFAULT

        coordinator.decide(row(1L), now) shouldBe DigestDecision.SEND_NOW

        verify(exactly = 0) { appendPort.saveAll(any()) }
        verify(exactly = 0) { digestPort.markDeferred(any(), any()) }
    }

    @Test
    fun `모아 받기가 켜져 있고 윈도우가 없으면 바로 보내고 10분 뒤 예약 행을 만든다`() {
        every { settingsQueryPort.findByMemberId(42L) } returns NotificationSettings(true, true)
        every { digestPort.findOpenDigestKey(42L, 1L, date) } returns null
        val saved = slot<List<NotificationOutbox>>()
        every { appendPort.saveAll(capture(saved)) } returns Unit

        coordinator.decide(row(1L), now) shouldBe DigestDecision.SEND_NOW

        val digest = saved.captured.single()
        digest.eventType shouldBe NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST
        digest.receiverMemberId shouldBe 42L
        digest.scheduledAt shouldBe now.plusMinutes(10)
    }

    @Test
    fun `윈도우가 열려 있으면 DEFERRED 로 미루고 예약 행에 연결한다`() {
        every { settingsQueryPort.findByMemberId(42L) } returns NotificationSettings(true, true)
        every { digestPort.findOpenDigestKey(42L, 1L, date) } returns "activity-digest:42:1:2026-11-03:20261103100000"

        coordinator.decide(row(2L), now) shouldBe DigestDecision.DEFER

        verify { digestPort.markDeferred(2L, "activity-digest:42:1:2026-11-03:20261103100000") }
        verify(exactly = 0) { appendPort.saveAll(any()) }
    }

    @Test
    fun `다른 그룹의 활동은 그룹 1 의 열린 윈도우에 묶이지 않고 자기 그룹 윈도우로 판단한다`() {
        every { settingsQueryPort.findByMemberId(42L) } returns NotificationSettings(true, true)
        every { digestPort.findOpenDigestKey(42L, 1L, date) } returns "activity-digest:42:1:2026-11-03:20261103100000"
        every { digestPort.findOpenDigestKey(42L, 2L, date) } returns null
        val saved = slot<List<NotificationOutbox>>()
        every { appendPort.saveAll(capture(saved)) } returns Unit
        val group2Row =
            NotificationOutbox.reconstitute(
                id = 3L,
                aggregateType = "TOPPING",
                aggregateId = 3L,
                eventType = "TOPPING_PLACED",
                receiverMemberId = 42L,
                payload = payload.copy(groupId = 2L),
                dedupKey = "k3",
                status = OutboxStatus.PENDING,
                attempts = 0,
                scheduledAt = now,
                lastError = null,
                createdAt = now,
                sentAt = null,
            )

        coordinator.decide(group2Row, now) shouldBe DigestDecision.SEND_NOW

        verify { digestPort.findOpenDigestKey(42L, 2L, date) }
        verify(exactly = 0) { digestPort.markDeferred(any(), any()) }
        saved.captured
            .single()
            .payload.groupId shouldBe 2L
    }

    @Test
    fun `윈도우 종료가 캔버스 마감 03시를 넘기면 03시로 당긴다`() {
        every { settingsQueryPort.findByMemberId(42L) } returns NotificationSettings(true, true)
        every { digestPort.findOpenDigestKey(42L, 1L, date) } returns null
        val saved = slot<List<NotificationOutbox>>()
        every { appendPort.saveAll(capture(saved)) } returns Unit
        val lateNight = LocalDateTime.of(2026, 11, 4, 2, 58, 0)

        coordinator.decide(row(1L), lateNight) shouldBe DigestDecision.SEND_NOW

        saved.captured.single().scheduledAt shouldBe LocalDateTime.of(2026, 11, 4, 3, 0)
    }

    @Test
    fun `재집계는 삭제된 사진과 텍스트를 빼고 배경 변경은 항상 센다`() {
        val toppingAlive = row(10L, "TOPPING_PLACED", aggregateId = 100L)
        val toppingGone = row(11L, "TOPPING_PLACED", aggregateId = 101L)
        val textAlive = row(12L, "TEXT_PLACED", aggregateId = 200L)
        val textGone = row(13L, "TEXT_PLACED", aggregateId = 201L)
        val background = row(14L, "BACKGROUND_CHANGED", aggregateId = 5L)
        every { digestPort.findDeferred("ref") } returns
            listOf(toppingAlive, toppingGone, textAlive, textGone, background)
        every { imageQueryPort.findAllByIds(listOf(100L, 101L)) } returns listOf(imageWithId(100L))
        every { textQueryPort.findAllByIds(listOf(200L, 201L)) } returns listOf(textWithId(200L))

        coordinator.countAlive("ref") shouldBe 3
    }

    @Test
    fun `대기 행이 없으면 재집계 결과는 0이다`() {
        every { digestPort.findDeferred("ref") } returns emptyList()

        coordinator.countAlive("ref") shouldBe 0
    }
}

private fun imageWithId(id: Long): ParfaitImage = mockk { every { this@mockk.id } returns id }

private fun textWithId(id: Long): ParfaitText = mockk { every { this@mockk.id } returns id }
