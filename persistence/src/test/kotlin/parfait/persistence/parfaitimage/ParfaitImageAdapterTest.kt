package parfait.persistence.parfaitimage

import io.kotest.matchers.shouldBe
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
import parfait.persistence.repository.ImageMetaRepository
import parfait.persistence.repository.MemberRepository
import parfait.persistence.repository.ParfaitGroupMemberRepository
import parfait.persistence.repository.ParfaitGroupRepository
import parfait.persistence.repository.ParfaitRepository
import java.time.LocalDate

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class ParfaitImageAdapterTest {
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
    private lateinit var parfaitImageAdapter: ParfaitImageAdapter

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

    private fun setUpGroupMember(suffix: String): Pair<Long, Long> {
        val member =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "parfait-image-test-$suffix",
                    globalNickname = "테스터$suffix",
                ),
            )
        val group =
            parfaitGroupRepository.save(
                ParfaitGroup(name = "그룹$suffix", inviteCode = "CODE$suffix", memberLimit = 12),
            )
        val groupMember =
            parfaitGroupMemberRepository.save(
                ParfaitGroupMember(
                    parfaitGroupId = requireNotNull(group.id),
                    memberId = requireNotNull(member.id),
                    groupNickname = "닉네임$suffix",
                ),
            )
        return requireNotNull(member.id) to requireNotNull(groupMember.id)
    }

    private fun setUpParfait(groupMemberId: Long): Long {
        val group = parfaitGroupMemberRepository.findById(groupMemberId).get()
        val parfait =
            parfaitRepository.save(
                Parfait(parfaitGroupId = group.parfaitGroupId, parfaitDate = LocalDate.now()),
            )
        return requireNotNull(parfait.id)
    }

    private fun setUpConfirmedImageMeta(
        memberId: Long,
        suffix: String,
    ): Long {
        val imageMeta =
            imageMetaRepository.save(
                ImageMeta(
                    url = "https://s3.example/nukki/user1/$suffix.png",
                    uploadedByMemberId = memberId,
                    imageType = "NUKKI",
                    status = "COMPLETED",
                ),
            )
        return requireNotNull(imageMeta.id)
    }

    @Test
    fun `저장하면 id가 채워진 도메인 객체를 반환한다`() {
        val (memberId, groupMemberId) = setUpGroupMember("A")
        val parfaitId = setUpParfait(groupMemberId)
        val imageMetaId = setUpConfirmedImageMeta(memberId, "A")

        val saved =
            parfaitImageAdapter.save(
                ParfaitImage.place(
                    parfaitId = parfaitId,
                    imageMetaId = imageMetaId,
                    placedByGroupMemberId = groupMemberId,
                    imageUrl = "https://s3.example/nukki/user1/uuid.png",
                    positionX = 120.5,
                    positionY = 340.2,
                    positionZ = 1,
                    scale = 1.0,
                    rotation = 0.0,
                    borderType = BorderType.NONE,
                    borderColor = null,
                    borderWidth = null,
                ),
            )

        saved.requireId() shouldBe saved.id
        saved.positionX shouldBe 120.5
        saved.borderType shouldBe BorderType.NONE
    }

    @Test
    fun `parfaitId와 imageMetaId로 조회하면 저장된 배치를 반환한다`() {
        val (memberId, groupMemberId) = setUpGroupMember("B")
        val parfaitId = setUpParfait(groupMemberId)
        val imageMetaId = setUpConfirmedImageMeta(memberId, "B")
        val saved =
            parfaitImageAdapter.save(
                ParfaitImage.place(
                    parfaitId = parfaitId,
                    imageMetaId = imageMetaId,
                    placedByGroupMemberId = groupMemberId,
                    imageUrl = "https://s3.example/nukki/user1/uuid2.png",
                    positionX = 0.0,
                    positionY = 0.0,
                    positionZ = 0,
                    scale = 1.0,
                    rotation = 0.0,
                    borderType = BorderType.SOLID,
                    borderColor = "#000000",
                    borderWidth = 2.0,
                ),
            )

        val found = parfaitImageAdapter.findByParfaitIdAndImageMetaId(parfaitId, imageMetaId)

        found?.id shouldBe saved.id
        found?.borderColor shouldBe "#000000"
    }

    @Test
    fun `존재하지 않는 조합으로 조회하면 null을 반환한다`() {
        parfaitImageAdapter.findByParfaitIdAndImageMetaId(-1L, -1L) shouldBe null
    }

    @Test
    fun `id로 조회하면 저장된 배치를 반환한다`() {
        val (memberId, groupMemberId) = setUpGroupMember("C")
        val parfaitId = setUpParfait(groupMemberId)
        val imageMetaId = setUpConfirmedImageMeta(memberId, "C")
        val saved =
            parfaitImageAdapter.save(
                ParfaitImage.place(
                    parfaitId = parfaitId,
                    imageMetaId = imageMetaId,
                    placedByGroupMemberId = groupMemberId,
                    imageUrl = "https://s3.example/nukki/user1/uuid3.png",
                    positionX = 10.0,
                    positionY = 20.0,
                    positionZ = 0,
                    scale = 1.0,
                    rotation = 0.0,
                    borderType = BorderType.NONE,
                    borderColor = null,
                    borderWidth = null,
                ),
            )

        val found = parfaitImageAdapter.findById(saved.requireId())

        found?.id shouldBe saved.id
        found?.positionX shouldBe 10.0
    }

    @Test
    fun `존재하지 않는 id로 조회하면 null을 반환한다`() {
        parfaitImageAdapter.findById(-1L) shouldBe null
    }

    @Test
    fun `parfaitId 목록으로 배치된 토핑 수를 그룹핑해 반환하고 배치가 없는 id는 결과에서 제외한다`() {
        val (memberId, groupMemberId) = setUpGroupMember("D")
        val (_, otherGroupMemberId) = setUpGroupMember("E")
        val parfaitIdWithImages = setUpParfait(groupMemberId)
        val parfaitIdWithoutImages = setUpParfait(otherGroupMemberId)
        val imageMetaIdA = setUpConfirmedImageMeta(memberId, "D1")
        val imageMetaIdB = setUpConfirmedImageMeta(memberId, "D2")
        parfaitImageAdapter.save(
            ParfaitImage.place(
                parfaitId = parfaitIdWithImages,
                imageMetaId = imageMetaIdA,
                placedByGroupMemberId = groupMemberId,
                imageUrl = "https://s3.example/nukki/user1/uuid4.png",
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
        parfaitImageAdapter.save(
            ParfaitImage.place(
                parfaitId = parfaitIdWithImages,
                imageMetaId = imageMetaIdB,
                placedByGroupMemberId = groupMemberId,
                imageUrl = "https://s3.example/nukki/user1/uuid5.png",
                positionX = 1.0,
                positionY = 1.0,
                positionZ = 1,
                scale = 1.0,
                rotation = 0.0,
                borderType = BorderType.NONE,
                borderColor = null,
                borderWidth = null,
            ),
        )

        val result =
            parfaitImageAdapter.countAllByParfaitIds(listOf(parfaitIdWithImages, parfaitIdWithoutImages))

        result shouldBe mapOf(parfaitIdWithImages to 2)
    }

    @Test
    fun `빈 목록으로 조회하면 빈 맵을 반환한다`() {
        parfaitImageAdapter.countAllByParfaitIds(emptyList()) shouldBe emptyMap()
    }
}
