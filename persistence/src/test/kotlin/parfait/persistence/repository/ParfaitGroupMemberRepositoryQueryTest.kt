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

        val summaries = parfaitGroupMemberRepository.findMyGroupSummaries(requireNotNull(creator.id))

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
                    parfaitDate = LocalDate.now(),
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

        val summaries = parfaitGroupMemberRepository.findMyGroupSummaries(requireNotNull(creator.id))

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
}
