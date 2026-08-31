package parfait.persistence.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.core.parfait.domain.ParfaitDay
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.persistence.TestApplication
import parfait.persistence.entity.ImageMeta
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.entity.Parfait
import parfait.persistence.entity.ParfaitGroup
import parfait.persistence.entity.ParfaitGroupMember
import parfait.persistence.parfaitimage.ParfaitImageAdapter
import java.time.LocalDate
import java.time.LocalDateTime

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class ParfaitGroupMemberRepositoryQueryTest {
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

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var parfaitGroupRepository: ParfaitGroupRepository

    @Autowired
    private lateinit var parfaitGroupMemberRepository: ParfaitGroupMemberRepository

    @Autowired
    private lateinit var parfaitRepository: ParfaitRepository

    @Autowired
    private lateinit var imageMetaRepository: ImageMetaRepository

    @Autowired
    private lateinit var parfaitImageAdapter: ParfaitImageAdapter

    private val today: LocalDate = ParfaitDay.current()

    @Test
    fun `토핑이 없는 그룹은 생성자의 chip과 그룹 생성 시각을 대신 반환한다`() {
        val creator =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "summary-test-creator",
                    globalNickname = "생성자",
                ),
            )
        val group =
            parfaitGroupRepository.save(
                ParfaitGroup(name = "새 그룹", inviteCode = "SUMM01", memberLimit = 12),
            )
        parfaitGroupMemberRepository.save(
            ParfaitGroupMember(
                parfaitGroupId = requireNotNull(group.id),
                memberId = requireNotNull(creator.id),
                groupNickname = "생성자닉네임",
                nametagChip = "TYPE3",
            ),
        )

        val summaries = parfaitGroupMemberRepository.findMyGroupSummaries(requireNotNull(creator.id), today)

        val summary = summaries.single { it.groupId == group.id }
        summary.recentImageUrl shouldBe null
        summary.recentImageUploadedAt shouldNotBe null
        summary.lastPlacedByNametagChip shouldBe "TYPE3"
    }

    @Test
    fun `토핑이 있으면 배치한 사람의 chip과 이미지 정보를 반환한다`() {
        val creator =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "summary-test-creator-2",
                    globalNickname = "생성자2",
                ),
            )
        val placerMember =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "summary-test-placer",
                    globalNickname = "배치자",
                ),
            )
        val group =
            parfaitGroupRepository.save(
                ParfaitGroup(name = "토핑 그룹", inviteCode = "SUMM02", memberLimit = 12),
            )
        parfaitGroupMemberRepository.save(
            ParfaitGroupMember(
                parfaitGroupId = requireNotNull(group.id),
                memberId = requireNotNull(creator.id),
                groupNickname = "생성자닉네임",
                nametagChip = "TYPE3",
            ),
        )
        val placerGroupMember =
            parfaitGroupMemberRepository.save(
                ParfaitGroupMember(
                    parfaitGroupId = requireNotNull(group.id),
                    memberId = requireNotNull(placerMember.id),
                    groupNickname = "배치자닉네임",
                    nametagChip = "TYPE5",
                ),
            )
        val parfait =
            parfaitRepository.save(
                Parfait(
                    parfaitGroupId = requireNotNull(group.id),
                    parfaitDate = today,
                    status = "ACTIVE",
                    backgroundType = null,
                    backgroundValue = null,
                ),
            )
        val imageMeta =
            imageMetaRepository.save(
                ImageMeta(
                    url = "https://s3.example/nukki/placer/topping.png",
                    uploadedByMemberId = requireNotNull(placerMember.id),
                    imageType = "NUKKI",
                    status = "COMPLETED",
                ),
            )
        val placedImage =
            parfaitImageAdapter.save(
                ParfaitImage.place(
                    parfaitId = requireNotNull(parfait.id),
                    imageMetaId = requireNotNull(imageMeta.id),
                    placedByGroupMemberId = requireNotNull(placerGroupMember.id),
                    imageUrl = "https://s3.example/nukki/placer/topping.png",
                    positionX = 0.0,
                    positionY = 0.0,
                    positionZ = 0,
                    scale = 1.0,
                    rotation = 0.0,
                    borderType = BorderType.NONE,
                    borderColor = null,
                    borderWidth = null,
                ),
            )

        val summaries = parfaitGroupMemberRepository.findMyGroupSummaries(requireNotNull(creator.id), today)

        val summary = summaries.single { it.groupId == group.id }
        summary.recentImageUrl shouldBe "https://s3.example/nukki/placer/topping.png"
        // MySQL DATETIME은 초 단위로 반올림 저장되므로(placedImage.createdAt은 나노초 포함),
        // 정확한 값 비교 대신 초 단위 오차(최대 1초)까지만 허용한다.
        val diffSeconds =
            java.time.Duration
                .between(placedImage.createdAt, summary.recentImageUploadedAt)
                .abs()
                .seconds
        (diffSeconds <= 1) shouldBe true
        summary.lastPlacedByNametagChip shouldBe "TYPE5"
    }

    @Test
    fun `전날 토핑만 있고 오늘 캔버스는 비면 마지막 토핑 시각·chip을 쓰고 recentImageUrl은 null이다`() {
        val creator = saveMember("recent-prev-creator", "생성자")
        val placer = saveMember("recent-prev-placer", "배치자")
        val group = parfaitGroupRepository.save(ParfaitGroup(name = "전날그룹", inviteCode = "PREV01", memberLimit = 12))
        saveGroupMember(requireNotNull(group.id), requireNotNull(creator.id), "생성자닉", "TYPE3")
        val placerGm = saveGroupMember(requireNotNull(group.id), requireNotNull(placer.id), "배치자닉", "TYPE5")

        val yesterdayParfait = saveParfait(requireNotNull(group.id), today.minusDays(1), "CLOSED")
        saveParfait(requireNotNull(group.id), today, "ACTIVE") // 오늘 캔버스: 토핑 없음
        val toppingTime = LocalDateTime.now().minusDays(1)
        placeTopping(
            parfaitId = requireNotNull(yesterdayParfait.id),
            placerGroupMemberId = requireNotNull(placerGm.id),
            uploaderMemberId = requireNotNull(placer.id),
            imageUrl = "https://s3.example/prev/topping.png",
            createdAt = toppingTime,
        )

        val summary =
            parfaitGroupMemberRepository
                .findMyGroupSummaries(requireNotNull(creator.id), today)
                .single { it.groupId == group.id }

        summary.recentImageUrl shouldBe null
        val diffSeconds =
            java.time.Duration
                .between(toppingTime, summary.recentImageUploadedAt)
                .abs()
                .seconds
        (diffSeconds <= 1) shouldBe true
        summary.lastPlacedByNametagChip shouldBe "TYPE5"
    }

    @Test
    fun `전날·오늘 캔버스가 비고 전전날에만 토핑이 있으면 전전날 마지막 토핑을 기준으로 한다`() {
        val creator = saveMember("recent-dbf-creator", "생성자")
        val placer = saveMember("recent-dbf-placer", "배치자")
        val group = parfaitGroupRepository.save(ParfaitGroup(name = "전전날그룹", inviteCode = "DBF001", memberLimit = 12))
        saveGroupMember(requireNotNull(group.id), requireNotNull(creator.id), "생성자닉", "TYPE3")
        val placerGm = saveGroupMember(requireNotNull(group.id), requireNotNull(placer.id), "배치자닉", "TYPE6")

        val dayBeforeParfait = saveParfait(requireNotNull(group.id), today.minusDays(2), "CLOSED")
        saveParfait(requireNotNull(group.id), today.minusDays(1), "CLOSED") // 어제: 빈 캔버스
        saveParfait(requireNotNull(group.id), today, "ACTIVE") // 오늘: 빈 캔버스
        val toppingTime = LocalDateTime.now().minusDays(2)
        placeTopping(
            parfaitId = requireNotNull(dayBeforeParfait.id),
            placerGroupMemberId = requireNotNull(placerGm.id),
            uploaderMemberId = requireNotNull(placer.id),
            imageUrl = "https://s3.example/dbf/topping.png",
            createdAt = toppingTime,
        )

        val summary =
            parfaitGroupMemberRepository
                .findMyGroupSummaries(requireNotNull(creator.id), today)
                .single { it.groupId == group.id }

        summary.recentImageUrl shouldBe null
        val diffSeconds =
            java.time.Duration
                .between(toppingTime, summary.recentImageUploadedAt)
                .abs()
                .seconds
        (diffSeconds <= 1) shouldBe true
        summary.lastPlacedByNametagChip shouldBe "TYPE6"
    }

    @Test
    fun `오늘 캔버스에 토핑이 여러 개면 가장 최근 토핑의 이미지를 recentImageUrl로 반환한다`() {
        val creator = saveMember("recent-today-creator", "생성자")
        val group = parfaitGroupRepository.save(ParfaitGroup(name = "오늘그룹", inviteCode = "TDY001", memberLimit = 12))
        val creatorGm = saveGroupMember(requireNotNull(group.id), requireNotNull(creator.id), "생성자닉", "TYPE3")

        val todayParfait = saveParfait(requireNotNull(group.id), today, "ACTIVE")
        val base = LocalDateTime.now().minusHours(2)
        placeTopping(
            parfaitId = requireNotNull(todayParfait.id),
            placerGroupMemberId = requireNotNull(creatorGm.id),
            uploaderMemberId = requireNotNull(creator.id),
            imageUrl = "https://s3.example/today/first.png",
            createdAt = base,
        )
        placeTopping(
            parfaitId = requireNotNull(todayParfait.id),
            placerGroupMemberId = requireNotNull(creatorGm.id),
            uploaderMemberId = requireNotNull(creator.id),
            imageUrl = "https://s3.example/today/second.png",
            createdAt = base.plusMinutes(1),
        )

        val summary =
            parfaitGroupMemberRepository
                .findMyGroupSummaries(requireNotNull(creator.id), today)
                .single { it.groupId == group.id }

        summary.recentImageUrl shouldBe "https://s3.example/today/second.png"
        val diffSeconds =
            java.time.Duration
                .between(base.plusMinutes(1), summary.recentImageUploadedAt)
                .abs()
                .seconds
        (diffSeconds <= 1) shouldBe true
    }

    @Test
    fun `오늘 캔버스가 빈 활동 그룹은 그룹 생성 시각이 아니라 마지막 토핑 시각으로 정렬된다`() {
        val me = saveMember("recent-sort3-me", "나")

        // 활동 그룹: 10일 전 생성, 오늘 캔버스는 비어 있고 어제 캔버스에 토핑이 있다.
        // 버그가 있으면 빈 오늘 캔버스 때문에 정렬 키가 g.created_at(10일 전)으로 새어나간다.
        val activeGroup =
            parfaitGroupRepository.save(
                ParfaitGroup(
                    name = "활동그룹",
                    inviteCode = "SRT3AC",
                    memberLimit = 12,
                    createdAt = LocalDateTime.now().minusDays(10),
                    updatedAt = LocalDateTime.now().minusDays(10),
                ),
            )
        val activeGm = saveGroupMember(requireNotNull(activeGroup.id), requireNotNull(me.id), "나닉A", "TYPE3")
        val activeYesterday = saveParfait(requireNotNull(activeGroup.id), today.minusDays(1), "CLOSED")
        saveParfait(requireNotNull(activeGroup.id), today, "ACTIVE") // 오늘: 빈 캔버스
        placeTopping(
            parfaitId = requireNotNull(activeYesterday.id),
            placerGroupMemberId = requireNotNull(activeGm.id),
            uploaderMemberId = requireNotNull(me.id),
            imageUrl = "https://s3.example/sort3/active.png",
            createdAt = LocalDateTime.now().minusDays(1),
        )

        // 조용한 그룹: 5일 전 생성, 캔버스도 토핑도 없음 → 정렬 키 = g.created_at(5일 전).
        val quietGroup =
            parfaitGroupRepository.save(
                ParfaitGroup(
                    name = "조용한그룹",
                    inviteCode = "SRT3QT",
                    memberLimit = 12,
                    createdAt = LocalDateTime.now().minusDays(5),
                    updatedAt = LocalDateTime.now().minusDays(5),
                ),
            )
        saveGroupMember(requireNotNull(quietGroup.id), requireNotNull(me.id), "나닉Q", "TYPE4")

        // 방금 만든 그룹: 토핑 없음 → 정렬 키 = g.created_at(지금).
        val freshGroup =
            parfaitGroupRepository.save(
                ParfaitGroup(
                    name = "방금그룹",
                    inviteCode = "SRT3FR",
                    memberLimit = 12,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )
        saveGroupMember(requireNotNull(freshGroup.id), requireNotNull(me.id), "나닉F", "TYPE5")

        val orderedGroupIds =
            parfaitGroupMemberRepository
                .findMyGroupSummaries(requireNotNull(me.id), today)
                .map { it.groupId }
                .filter { it == activeGroup.id || it == quietGroup.id || it == freshGroup.id }

        // 수정 후 정렬 키: fresh(지금) > active(어제 토핑) > quiet(5일 전 생성).
        // 버그 상태에선 active 키가 10일 전(g.created_at)이 되어 quiet 뒤로 밀린다.
        orderedGroupIds shouldBe listOf(freshGroup.id, activeGroup.id, quietGroup.id)
    }

    private fun saveMember(
        providerUserId: String,
        nickname: String,
    ): Member =
        memberRepository.save(
            Member(
                loginProvider = LoginProvider.KAKAO,
                providerUserId = providerUserId,
                globalNickname = nickname,
            ),
        )

    private fun saveGroupMember(
        groupId: Long,
        memberId: Long,
        nickname: String,
        chip: String,
    ): ParfaitGroupMember =
        parfaitGroupMemberRepository.save(
            ParfaitGroupMember(
                parfaitGroupId = groupId,
                memberId = memberId,
                groupNickname = nickname,
                nametagChip = chip,
            ),
        )

    private fun saveParfait(
        groupId: Long,
        date: LocalDate,
        status: String,
    ): Parfait =
        parfaitRepository.save(
            Parfait(
                parfaitGroupId = groupId,
                parfaitDate = date,
                status = status,
                backgroundType = null,
                backgroundValue = null,
            ),
        )

    // 지정한 parfait 캔버스에 토핑 1개를 올린다. createdAt을 명시적으로 넣어 정렬/시각 단언을 결정적으로 만든다.
    private fun placeTopping(
        parfaitId: Long,
        placerGroupMemberId: Long,
        uploaderMemberId: Long,
        imageUrl: String,
        createdAt: LocalDateTime,
    ): ParfaitImage {
        val imageMeta =
            imageMetaRepository.save(
                ImageMeta(
                    url = imageUrl,
                    uploadedByMemberId = uploaderMemberId,
                    imageType = "NUKKI",
                    status = "COMPLETED",
                ),
            )
        return parfaitImageAdapter.save(
            ParfaitImage.place(
                parfaitId = parfaitId,
                imageMetaId = requireNotNull(imageMeta.id),
                placedByGroupMemberId = placerGroupMemberId,
                imageUrl = imageUrl,
                positionX = 0.0,
                positionY = 0.0,
                positionZ = 0,
                scale = 1.0,
                rotation = 0.0,
                borderType = BorderType.NONE,
                borderColor = null,
                borderWidth = null,
                now = createdAt,
            ),
        )
    }
}
