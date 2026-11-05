package parfait.persistence.notification

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.core.notification.domain.BackgroundChangedPayload
import parfait.core.notification.domain.DevicePlatform
import parfait.core.notification.domain.DeviceToken
import parfait.core.notification.domain.NotificationMessageFactory
import parfait.core.notification.domain.NotificationOutbox
import parfait.core.notification.domain.OutboxStatus
import parfait.core.notification.domain.PushMessage
import parfait.core.notification.domain.TextPlacedPayload
import parfait.core.notification.domain.ToppingPlacedPayload
import parfait.core.notification.port.out.NotificationOutboxAppendPort
import parfait.core.notification.port.out.NotificationSenderPort
import parfait.core.notification.service.ActivityDigestCoordinator
import parfait.core.notification.service.NotificationOutboxDispatcher
import parfait.persistence.TestApplication
import parfait.persistence.entity.ImageMeta
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.entity.Parfait
import parfait.persistence.entity.ParfaitGroup
import parfait.persistence.entity.ParfaitGroupMember
import parfait.persistence.entity.ParfaitImage
import parfait.persistence.entity.ParfaitText
import parfait.persistence.repository.DeviceTokenRepository
import parfait.persistence.repository.ImageMetaRepository
import parfait.persistence.repository.MemberRepository
import parfait.persistence.repository.NotificationOutboxRepository
import parfait.persistence.repository.ParfaitGroupMemberRepository
import parfait.persistence.repository.ParfaitGroupRepository
import parfait.persistence.repository.ParfaitImageRepository
import parfait.persistence.repository.ParfaitRepository
import parfait.persistence.repository.ParfaitTextRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 디스패처 -> 코디네이터 -> 실제 MySQL outbox 로 이어지는 활동 묶음 알림(P-06) 흐름 검증.
 * FCM 발송(NotificationSenderPort)만 기록용 가짜로 대체한다.
 */
@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
@Import(
    ActivityDigestFlowIntegrationTest.FakeSenderConfig::class,
    NotificationOutboxDispatcher::class,
    ActivityDigestCoordinator::class,
    NotificationMessageFactory::class,
)
class ActivityDigestFlowIntegrationTest {
    class RecordingSender : NotificationSenderPort {
        val sent = CopyOnWriteArrayList<Pair<String, PushMessage>>()

        override fun send(
            token: String,
            message: PushMessage,
        ) {
            sent += token to message
        }

        override fun sendMulticast(
            tokens: List<String>,
            message: PushMessage,
        ) = throw UnsupportedOperationException()
    }

    @org.springframework.boot.test.context.TestConfiguration
    class FakeSenderConfig {
        @Bean
        fun recordingSender(): RecordingSender = RecordingSender()
    }

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.4")

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
        }
    }

    @Autowired private lateinit var dispatcher: NotificationOutboxDispatcher

    @Autowired private lateinit var sender: RecordingSender

    @Autowired private lateinit var appendPort: NotificationOutboxAppendPort

    @Autowired private lateinit var settingsAdapter: NotificationSettingsAdapter

    @Autowired private lateinit var deviceTokenAdapter: DeviceTokenAdapter

    @Autowired private lateinit var outboxRepository: NotificationOutboxRepository

    @Autowired private lateinit var memberRepository: MemberRepository

    @Autowired private lateinit var groupRepository: ParfaitGroupRepository

    @Autowired private lateinit var groupMemberRepository: ParfaitGroupMemberRepository

    @Autowired private lateinit var deviceTokenRepository: DeviceTokenRepository

    @Autowired private lateinit var parfaitRepository: ParfaitRepository

    @Autowired private lateinit var parfaitImageRepository: ParfaitImageRepository

    @Autowired private lateinit var imageMetaRepository: ImageMetaRepository

    @Autowired private lateinit var parfaitTextRepository: ParfaitTextRepository

    private val day = LocalDate.of(2026, 9, 2)

    private fun at(
        hour: Int,
        minute: Int,
    ): LocalDateTime = day.atTime(hour, minute)

    @BeforeEach
    fun resetSender() {
        sender.sent.clear()
    }

    @AfterEach
    fun clear() {
        outboxRepository.deleteAll()
        parfaitImageRepository.deleteAll()
        parfaitTextRepository.deleteAll()
        imageMetaRepository.deleteAll()
        parfaitRepository.deleteAll()
        deviceTokenRepository.deleteAll()
        groupMemberRepository.deleteAll()
        groupRepository.deleteAll()
        memberRepository.deleteAll()
    }

    private fun newMember(tag: String): Long =
        memberRepository
            .save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "adf-$tag-${System.nanoTime()}",
                    globalNickname = "묶음$tag",
                ),
            ).id!!

    private fun newGroup(code: String): Long =
        groupRepository.save(ParfaitGroup(name = "그룹$code", inviteCode = code, memberLimit = 12)).id!!

    private fun join(
        groupId: Long,
        memberId: Long,
    ): Long =
        groupMemberRepository
            .save(
                ParfaitGroupMember(
                    parfaitGroupId = groupId,
                    memberId = memberId,
                    groupNickname = "닉$memberId",
                ),
            ).id!!

    private fun addToken(
        memberId: Long,
        token: String,
    ) = deviceTokenAdapter.save(
        DeviceToken.register(memberId = memberId, sessionId = null, token = token, platform = DevicePlatform.ANDROID),
    )

    private fun digestOn(memberId: Long) = settingsAdapter.update(memberId, null, true)

    private fun parfaitIdOf(groupId: Long): Long =
        parfaitRepository
            .save(
                Parfait(
                    parfaitGroupId = groupId,
                    parfaitDate = day,
                    status = "OPEN",
                    backgroundType = null,
                    backgroundValue = null,
                ),
            ).id!!

    private fun topping(
        id: Long,
        receiver: Long,
        groupId: Long,
        actor: Long,
        now: LocalDateTime,
        parfaitId: Long = 1L,
    ) = NotificationOutbox.toppingPlaced(
        id,
        receiver,
        ToppingPlacedPayload(groupId, parfaitId, day, actor),
        now,
    )

    private fun text(
        id: Long,
        receiver: Long,
        groupId: Long,
        actor: Long,
        now: LocalDateTime,
    ) = NotificationOutbox.textPlaced(
        id,
        receiver,
        TextPlacedPayload(groupId, 1L, day, actor, "안녕하세요"),
        now,
    )

    private fun background(
        receiver: Long,
        groupId: Long,
        actor: Long,
        now: LocalDateTime,
    ) = NotificationOutbox.backgroundChanged(
        receiver,
        BackgroundChangedPayload(groupId, 1L, day, actor),
        now,
        now,
    )

    private fun rows(eventType: String) = outboxRepository.findAll().filter { it.eventType == eventType }

    @Test
    fun `모아 받기 ON이면 첫 알림은 즉시 가고 이후 활동은 묶음 1건으로 발송된다`() {
        val a = newMember("a")
        val b = newMember("b")
        val g = newGroup("ADF001")
        join(g, a)
        val bMember = join(g, b)
        addToken(a, "tok-a")
        digestOn(a)
        val textId = newText(parfaitIdOf(g), bMember)

        // 10:00 첫 활동 -> 즉시 발송 + 예약 행 생성
        appendPort.saveAll(listOf(topping(1L, a, g, b, at(10, 0))))
        dispatcher.processDueBatch(at(10, 0))

        sender.sent shouldHaveSize 1
        sender.sent[0].first shouldBe "tok-a"
        sender.sent[0].second.data["type"] shouldBe "TOPPING"
        val digest = rows(NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST)
        digest shouldHaveSize 1
        digest[0].status shouldBe OutboxStatus.PENDING
        digest[0].scheduledAt shouldBe at(10, 10)
        val digestKey = digest[0].dedupKey

        // 10:03 텍스트, 10:07 배경 -> 발송 없이 대기
        appendPort.saveAll(listOf(text(textId, a, g, b, at(10, 3))))
        dispatcher.processDueBatch(at(10, 3))
        appendPort.saveAll(listOf(background(a, g, b, at(10, 7))))
        dispatcher.processDueBatch(at(10, 7))

        sender.sent shouldHaveSize 1
        val deferred =
            rows(NotificationOutbox.EVENT_TYPE_TEXT_PLACED) + rows(NotificationOutbox.EVENT_TYPE_BACKGROUND_CHANGED)
        deferred shouldHaveSize 2
        deferred.forEach {
            it.status shouldBe OutboxStatus.DEFERRED
            it.digestRef shouldBe digestKey
        }

        // 10:10 예약 시각 -> 묶음 1건
        dispatcher.processDueBatch(at(10, 10))

        sender.sent shouldHaveSize 2
        val message = sender.sent[1].second
        message.data["type"] shouldBe "ACTIVITY_DIGEST"
        message.body shouldBe "그사이 새 소식이 2개 쌓였어요"
        rows(NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST).single().status shouldBe OutboxStatus.SENT
        val after =
            rows(NotificationOutbox.EVENT_TYPE_TEXT_PLACED) + rows(NotificationOutbox.EVENT_TYPE_BACKGROUND_CHANGED)
        after.forEach {
            it.status shouldBe OutboxStatus.SENT
            it.lastError shouldBe "DIGESTED"
        }
    }

    @Test
    fun `모아 받기 OFF면 활동마다 개별 발송되고 예약 행은 생기지 않는다`() {
        val a = newMember("a")
        val b = newMember("b")
        val g = newGroup("ADF010")
        join(g, a)
        join(g, b)
        addToken(a, "tok-a")

        appendPort.saveAll(listOf(topping(1L, a, g, b, at(10, 0))))
        dispatcher.processDueBatch(at(10, 0))
        appendPort.saveAll(listOf(text(2L, a, g, b, at(10, 3))))
        dispatcher.processDueBatch(at(10, 3))

        sender.sent shouldHaveSize 2
        sender.sent.map { it.second.data["type"] } shouldBe listOf("TOPPING", "TEXT")
        rows(NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST) shouldHaveSize 0
    }

    @Test
    fun `다른 그룹의 활동은 서로의 윈도우에 묶이지 않고 각각 즉시 발송된다`() {
        val a = newMember("a")
        val b = newMember("b")
        val gx = newGroup("ADF020")
        val gy = newGroup("ADF021")
        join(gx, a)
        join(gx, b)
        join(gy, a)
        join(gy, b)
        addToken(a, "tok-a")
        digestOn(a)

        appendPort.saveAll(listOf(topping(1L, a, gx, b, at(10, 0))))
        dispatcher.processDueBatch(at(10, 0))
        appendPort.saveAll(listOf(topping(2L, a, gy, b, at(10, 2))))
        dispatcher.processDueBatch(at(10, 2))

        sender.sent shouldHaveSize 2
        val digests = rows(NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST)
        digests shouldHaveSize 2
        digests.map { it.aggregateId }.toSet() shouldBe setOf(gx, gy)
    }

    @Test
    fun `대기 중 일부 활동이 삭제되면 삭제분을 뺀 개수로 묶음 발송된다`() {
        val a = newMember("a")
        val b = newMember("b")
        val g = newGroup("ADF030")
        join(g, a)
        val bMember = join(g, b)
        addToken(a, "tok-a")
        digestOn(a)
        val parfaitId = parfaitIdOf(g)
        val img1 = newImage(parfaitId, b, bMember, "u1")
        val img2 = newImage(parfaitId, b, bMember, "u2")
        val img3 = newImage(parfaitId, b, bMember, "u3")

        appendPort.saveAll(listOf(topping(img1, a, g, b, at(10, 0), parfaitId)))
        dispatcher.processDueBatch(at(10, 0))
        appendPort.saveAll(listOf(topping(img2, a, g, b, at(10, 2), parfaitId)))
        dispatcher.processDueBatch(at(10, 2))
        appendPort.saveAll(listOf(topping(img3, a, g, b, at(10, 4), parfaitId)))
        dispatcher.processDueBatch(at(10, 4))
        sender.sent shouldHaveSize 1

        parfaitImageRepository.deleteById(img2)
        dispatcher.processDueBatch(at(10, 10))

        sender.sent shouldHaveSize 2
        sender.sent[1].second.body shouldBe "그사이 새 소식이 1개 쌓였어요"
        rows(NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST).single().status shouldBe OutboxStatus.SENT
    }

    @Test
    fun `대기 중 활동이 모두 삭제되면 발송 없이 예약 행이 종료되고 대기 행이 정리된다`() {
        val a = newMember("a")
        val b = newMember("b")
        val g = newGroup("ADF031")
        join(g, a)
        val bMember = join(g, b)
        addToken(a, "tok-a")
        digestOn(a)
        val parfaitId = parfaitIdOf(g)
        val img1 = newImage(parfaitId, b, bMember, "u1")
        val img2 = newImage(parfaitId, b, bMember, "u2")

        appendPort.saveAll(listOf(topping(img1, a, g, b, at(10, 0), parfaitId)))
        dispatcher.processDueBatch(at(10, 0))
        appendPort.saveAll(listOf(topping(img2, a, g, b, at(10, 3), parfaitId)))
        dispatcher.processDueBatch(at(10, 3))
        sender.sent shouldHaveSize 1

        parfaitImageRepository.deleteById(img2)
        dispatcher.processDueBatch(at(10, 5)) // 아직 예약 시각 전: 변화 없음
        sender.sent shouldHaveSize 1
        dispatcher.processDueBatch(at(10, 10))

        sender.sent shouldHaveSize 1
        val digest = rows(NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST).single()
        digest.status shouldBe OutboxStatus.SENT
        digest.lastError shouldBe "CANCELLED_EMPTY_DIGEST"
        val pending = rows(NotificationOutbox.EVENT_TYPE_TOPPING_PLACED).single { it.aggregateId == img2 }
        pending.status shouldBe OutboxStatus.SENT
        pending.lastError shouldBe "CANCELLED_EMPTY_DIGEST"
    }

    @Test
    fun `예약 시각 전에 모아 받기를 끄면 발송 없이 종료되고 대기 행이 정리된다`() {
        val a = newMember("a")
        val b = newMember("b")
        val g = newGroup("ADF040")
        join(g, a)
        join(g, b)
        addToken(a, "tok-a")
        digestOn(a)

        appendPort.saveAll(listOf(topping(1L, a, g, b, at(10, 0))))
        dispatcher.processDueBatch(at(10, 0))
        appendPort.saveAll(listOf(text(2L, a, g, b, at(10, 3))))
        dispatcher.processDueBatch(at(10, 3))
        sender.sent shouldHaveSize 1

        settingsAdapter.update(a, null, false)
        dispatcher.processDueBatch(at(10, 10))

        sender.sent shouldHaveSize 1
        val digest = rows(NotificationOutbox.EVENT_TYPE_ACTIVITY_DIGEST).single()
        digest.status shouldBe OutboxStatus.SENT
        digest.lastError shouldBe "CANCELLED_DIGEST_DISABLED"
        val pending = rows(NotificationOutbox.EVENT_TYPE_TEXT_PLACED).single()
        pending.status shouldBe OutboxStatus.SENT
        pending.lastError shouldBe "CANCELLED_DIGEST_DISABLED"
    }

    private fun newText(
        parfaitId: Long,
        groupMemberId: Long,
    ): Long =
        parfaitTextRepository
            .save(
                ParfaitText(
                    parfaitId = parfaitId,
                    placedByGroupMemberId = groupMemberId,
                    text = "안녕하세요",
                    positionX = 0.0,
                    positionY = 0.0,
                    positionZ = 1,
                    scale = 1.0,
                    rotation = 0.0,
                    color = "#000000",
                    fontSize = 14.0,
                ),
            ).id!!

    private fun newImage(
        parfaitId: Long,
        uploaderMemberId: Long,
        groupMemberId: Long,
        tag: String,
    ): Long {
        val meta =
            imageMetaRepository
                .save(
                    ImageMeta(
                        url = "https://example.com/$tag-${System.nanoTime()}",
                        uploadedByMemberId = uploaderMemberId,
                        imageType = "PHOTO",
                        status = "ACTIVE",
                    ),
                ).id!!
        return parfaitImageRepository
            .save(
                ParfaitImage(
                    parfaitId = parfaitId,
                    imageMetaId = meta,
                    placedByGroupMemberId = groupMemberId,
                    imageUrl = "https://example.com/$tag",
                    positionX = 0.0,
                    positionY = 0.0,
                    positionZ = 1,
                    scale = 1.0,
                    borderType = "NONE",
                    borderColor = null,
                    borderWidth = null,
                ),
            ).id!!
    }
}
