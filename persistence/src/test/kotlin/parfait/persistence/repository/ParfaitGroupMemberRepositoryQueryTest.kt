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
import parfait.core.parfaittext.domain.ParfaitText
import parfait.persistence.TestApplication
import parfait.persistence.entity.ImageMeta
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.entity.Parfait
import parfait.persistence.entity.ParfaitGroup
import parfait.persistence.entity.ParfaitGroupMember
import parfait.persistence.parfaitimage.ParfaitImageAdapter
import parfait.persistence.parfaittext.ParfaitTextAdapter
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

    @Autowired
    private lateinit var parfaitTextAdapter: ParfaitTextAdapter

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

    @Test
    fun `텍스트 포함 요약은 오늘 가장 최근 토핑이 텍스트면 TEXT 와 null 이미지 정보를, 시각과 chip 은 텍스트 기준으로 반환한다`() {
        val creator = saveMember("v2-text-creator", "생성자")
        val writer = saveMember("v2-text-writer", "작성자")
        val group = parfaitGroupRepository.save(ParfaitGroup(name = "텍스트최근", inviteCode = "V2T001", memberLimit = 12))
        val creatorGm = saveGroupMember(requireNotNull(group.id), requireNotNull(creator.id), "생성자닉", "TYPE3")
        val writerGm = saveGroupMember(requireNotNull(group.id), requireNotNull(writer.id), "작성자닉", "TYPE9")
        val todayParfait = saveParfait(requireNotNull(group.id), today, "ACTIVE")
        val base = LocalDateTime.now().minusHours(2)
        placeTopping(
            parfaitId = requireNotNull(todayParfait.id),
            placerGroupMemberId = requireNotNull(creatorGm.id),
            uploaderMemberId = requireNotNull(creator.id),
            imageUrl = "https://s3.example/v2/image.png",
            createdAt = base,
        )
        placeText(requireNotNull(todayParfait.id), requireNotNull(writerGm.id), base.plusMinutes(1))

        val summary =
            parfaitGroupMemberRepository
                .findMyGroupToppingSummaries(requireNotNull(creator.id), today)
                .single { it.groupId == group.id }

        summary.recentToppingType shouldBe "TEXT"
        summary.recentImageUrl shouldBe null
        summary.recentImageBorderType shouldBe null
        summary.recentImageBorderColor shouldBe null
        summary.recentImageBorderWidth shouldBe null
        summary.lastPlacedByNametagChip shouldBe "TYPE9"
        secondsBetween(base.plusMinutes(1), summary.recentImageUploadedAt) shouldBe 0L

        // v1 요약은 텍스트를 보지 않고 기존처럼 이미지만 기준으로 한다.
        val v1 =
            parfaitGroupMemberRepository
                .findMyGroupSummaries(requireNotNull(creator.id), today)
                .single { it.groupId == group.id }
        v1.recentImageUrl shouldBe "https://s3.example/v2/image.png"
        v1.lastPlacedByNametagChip shouldBe "TYPE3"
    }

    @Test
    fun `텍스트 포함 요약은 오늘 가장 최근 토핑이 이미지면 IMAGE 와 이미지 정보를 반환한다`() {
        val creator = saveMember("v2-image-creator", "생성자")
        val group = parfaitGroupRepository.save(ParfaitGroup(name = "이미지최근", inviteCode = "V2I001", memberLimit = 12))
        val creatorGm = saveGroupMember(requireNotNull(group.id), requireNotNull(creator.id), "생성자닉", "TYPE4")
        val todayParfait = saveParfait(requireNotNull(group.id), today, "ACTIVE")
        val base = LocalDateTime.now().minusHours(2)
        placeText(requireNotNull(todayParfait.id), requireNotNull(creatorGm.id), base)
        placeTopping(
            parfaitId = requireNotNull(todayParfait.id),
            placerGroupMemberId = requireNotNull(creatorGm.id),
            uploaderMemberId = requireNotNull(creator.id),
            imageUrl = "https://s3.example/v2/latest.png",
            createdAt = base.plusMinutes(1),
        )

        val summary =
            parfaitGroupMemberRepository
                .findMyGroupToppingSummaries(requireNotNull(creator.id), today)
                .single { it.groupId == group.id }

        summary.recentToppingType shouldBe "IMAGE"
        summary.recentImageUrl shouldBe "https://s3.example/v2/latest.png"
        summary.recentImageBorderType shouldBe "NONE"
        secondsBetween(base.plusMinutes(1), summary.recentImageUploadedAt) shouldBe 0L
    }

    @Test
    fun `텍스트 포함 요약은 토핑이 없으면 타입과 이미지가 null 이고 생성자 chip 과 그룹 생성 시각을 쓴다`() {
        val creator = saveMember("v2-empty-creator", "생성자")
        val group = parfaitGroupRepository.save(ParfaitGroup(name = "빈그룹", inviteCode = "V2E001", memberLimit = 12))
        saveGroupMember(requireNotNull(group.id), requireNotNull(creator.id), "생성자닉", "TYPE5")
        saveParfait(requireNotNull(group.id), today, "ACTIVE")

        val summary =
            parfaitGroupMemberRepository
                .findMyGroupToppingSummaries(requireNotNull(creator.id), today)
                .single { it.groupId == group.id }

        summary.recentToppingType shouldBe null
        summary.recentImageUrl shouldBe null
        summary.lastPlacedByNametagChip shouldBe "TYPE5"
        secondsBetween(group.createdAt, summary.recentImageUploadedAt) shouldBe 0L
    }

    @Test
    fun `텍스트 포함 요약은 전날 텍스트만 있으면 타입과 이미지는 null 이고 시각과 chip 은 그 텍스트 기준이다`() {
        val creator = saveMember("v2-yesterday-creator", "생성자")
        val group = parfaitGroupRepository.save(ParfaitGroup(name = "어제텍스트", inviteCode = "V2Y001", memberLimit = 12))
        val creatorGm = saveGroupMember(requireNotNull(group.id), requireNotNull(creator.id), "생성자닉", "TYPE6")
        val yesterdayParfait = saveParfait(requireNotNull(group.id), today.minusDays(1), "CLOSED")
        saveParfait(requireNotNull(group.id), today, "ACTIVE")
        val textAt = LocalDateTime.now().minusDays(1)
        placeText(requireNotNull(yesterdayParfait.id), requireNotNull(creatorGm.id), textAt)

        val summary =
            parfaitGroupMemberRepository
                .findMyGroupToppingSummaries(requireNotNull(creator.id), today)
                .single { it.groupId == group.id }

        summary.recentToppingType shouldBe null
        summary.recentImageUrl shouldBe null
        summary.lastPlacedByNametagChip shouldBe "TYPE6"
        secondsBetween(textAt, summary.recentImageUploadedAt) shouldBe 0L
    }

    @Test
    fun `텍스트 포함 요약은 방금 텍스트를 올린 그룹을 이전에 이미지를 올린 그룹보다 앞에 정렬한다`() {
        val me = saveMember("v2-order-me", "나")
        // 두 그룹 모두 3일 전에 생성해, 텍스트를 무시하면 텍스트 그룹의 정렬 키가 생성 시각(3일 전)으로 밀려나게 한다.
        val createdAt = LocalDateTime.now().minusDays(3)
        val imageGroup =
            parfaitGroupRepository.save(
                ParfaitGroup(
                    name = "이미지그룹",
                    inviteCode = "V2O001",
                    memberLimit = 12,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                ),
            )
        val textGroup =
            parfaitGroupRepository.save(
                ParfaitGroup(
                    name = "텍스트그룹",
                    inviteCode = "V2O002",
                    memberLimit = 12,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                ),
            )
        val imageGm = saveGroupMember(requireNotNull(imageGroup.id), requireNotNull(me.id), "나", "TYPE1")
        val textGm = saveGroupMember(requireNotNull(textGroup.id), requireNotNull(me.id), "나", "TYPE2")
        val base = LocalDateTime.now().minusHours(2)
        placeTopping(
            parfaitId = requireNotNull(saveParfait(requireNotNull(imageGroup.id), today, "ACTIVE").id),
            placerGroupMemberId = requireNotNull(imageGm.id),
            uploaderMemberId = requireNotNull(me.id),
            imageUrl = "https://s3.example/v2/order.png",
            createdAt = base,
        )
        placeText(
            requireNotNull(saveParfait(requireNotNull(textGroup.id), today, "ACTIVE").id),
            requireNotNull(textGm.id),
            base.plusMinutes(1),
        )

        val ids =
            parfaitGroupMemberRepository
                .findMyGroupToppingSummaries(
                    requireNotNull(me.id),
                    today,
                ).map { it.groupId }

        ids shouldBe listOf(textGroup.id, imageGroup.id)
        // v1은 텍스트를 무시하므로 텍스트 그룹은 그룹 생성 시각(3일 전) 기준으로 뒤에 정렬된다.
        parfaitGroupMemberRepository.findMyGroupSummaries(requireNotNull(me.id), today).map { it.groupId } shouldBe
            listOf(imageGroup.id, textGroup.id)
    }

    @Test
    fun `탈퇴한 멤버는 existsByParfaitGroupIdAndMemberIdAndLeftAtIsNull이 false를 반환한다`() {
        val member = saveMember("exists-left-member", "탈퇴자")
        val group = parfaitGroupRepository.save(ParfaitGroup(name = "탈퇴테스트그룹", inviteCode = "LEFT01", memberLimit = 12))
        val groupMember = saveGroupMember(requireNotNull(group.id), requireNotNull(member.id), "탈퇴자닉", "TYPE1")
        groupMember.leftAt = LocalDateTime.now()
        parfaitGroupMemberRepository.save(groupMember)

        val exists =
            parfaitGroupMemberRepository.existsByParfaitGroupIdAndMemberIdAndLeftAtIsNull(
                requireNotNull(group.id),
                requireNotNull(member.id),
            )

        exists shouldBe false
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

    private fun placeText(
        parfaitId: Long,
        placerGroupMemberId: Long,
        createdAt: LocalDateTime,
    ): ParfaitText =
        parfaitTextAdapter.save(
            ParfaitText.place(
                parfaitId = parfaitId,
                placedByGroupMemberId = placerGroupMemberId,
                text = "텍스트",
                positionX = 0.0,
                positionY = 0.0,
                positionZ = 0,
                scale = 1.0,
                rotation = 0.0,
                color = "#000000",
                fontSize = 16.0,
                now = createdAt,
            ),
        )

    private fun secondsBetween(
        expected: LocalDateTime,
        actual: LocalDateTime,
    ): Long =
        java.time.Duration
            .between(expected, actual)
            .abs()
            .seconds
            .let { if (it <= 1) 0L else it }
}
