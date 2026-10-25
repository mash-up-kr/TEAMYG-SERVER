package parfait.persistence.parfaittext

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.core.parfaittext.domain.ParfaitText
import parfait.persistence.TestApplication
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.entity.Parfait
import parfait.persistence.entity.ParfaitGroup
import parfait.persistence.entity.ParfaitGroupMember
import parfait.persistence.repository.MemberRepository
import parfait.persistence.repository.ParfaitGroupMemberRepository
import parfait.persistence.repository.ParfaitGroupRepository
import parfait.persistence.repository.ParfaitRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Testcontainers
@SpringBootTest(classes = [TestApplication::class], properties = ["spring.jpa.hibernate.ddl-auto=validate"])
class ParfaitTextAdapterTest {
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
    private lateinit var adapter: ParfaitTextAdapter

    @Autowired
    private lateinit var members: MemberRepository

    @Autowired
    private lateinit var groups: ParfaitGroupRepository

    @Autowired
    private lateinit var groupMembers: ParfaitGroupMemberRepository

    @Autowired
    private lateinit var parfaits: ParfaitRepository

    private fun fixture(suffix: String): Pair<Long, Long> {
        val member =
            members.save(
                Member(loginProvider = LoginProvider.KAKAO, providerUserId = "text-$suffix", globalNickname = "테스터"),
            )
        val group = groups.save(ParfaitGroup(name = "텍스트 테스트", inviteCode = "TXT$suffix", memberLimit = 12))
        val groupMember =
            groupMembers.save(
                ParfaitGroupMember(
                    parfaitGroupId = requireNotNull(group.id),
                    memberId = requireNotNull(member.id),
                    groupNickname = "테스터",
                ),
            )
        val parfait =
            parfaits.save(
                Parfait(
                    parfaitGroupId = requireNotNull(group.id),
                    parfaitDate = LocalDate.of(2026, 10, 20),
                    status = "ACTIVE",
                    backgroundType = null,
                    backgroundValue = null,
                ),
            )
        return requireNotNull(parfait.id) to requireNotNull(groupMember.id)
    }

    private fun text(
        parfaitId: Long,
        groupMemberId: Long,
        positionZ: Int = 2,
    ): ParfaitText =
        ParfaitText.place(
            parfaitId = parfaitId,
            placedByGroupMemberId = groupMemberId,
            text = "안녕\n파르페! 🎉",
            positionX = 120.5,
            positionY = 340.2,
            positionZ = positionZ,
            scale = 1.5,
            rotation = -30.0,
            color = "#FF5733",
            fontSize = 16.5,
            isFill = false,
            isStroke = true,
            now = LocalDateTime.of(2026, 10, 20, 14, 30),
        )

    @Test
    fun `마이그레이션으로 생성한 테이블에 텍스트와 스타일을 저장하고 조회한다`() {
        val (parfaitId, groupMemberId) = fixture("A")
        val saved = adapter.save(text(parfaitId, groupMemberId))

        val found = adapter.findAllByParfaitId(parfaitId).single()

        found.requireId() shouldBe saved.requireId()
        found.parfaitId shouldBe parfaitId
        found.placedByGroupMemberId shouldBe groupMemberId
        found.text shouldBe "안녕\n파르페! 🎉"
        found.positionX shouldBe 120.5
        found.positionY shouldBe 340.2
        found.positionZ shouldBe 2
        found.scale shouldBe 1.5
        found.rotation shouldBe -30.0
        found.color shouldBe "#FF5733"
        found.fontSize shouldBe 16.5
        found.isFill shouldBe false
        found.isStroke shouldBe true
        found.createdAt shouldBe saved.createdAt
        found.updatedAt shouldBe saved.updatedAt
        adapter.existsByParfaitId(parfaitId) shouldBe true
    }

    @Test
    fun `해당 파르페의 텍스트만 z 순서와 id 순서로 조회하고 같은 내용도 별도로 저장한다`() {
        val (parfaitId, groupMemberId) = fixture("B")
        val (otherParfaitId, otherGroupMemberId) = fixture("C")
        val high = adapter.save(text(parfaitId, groupMemberId, positionZ = 3))
        val low = adapter.save(text(parfaitId, groupMemberId, positionZ = 1))
        val sameZ = adapter.save(text(parfaitId, groupMemberId, positionZ = 1))
        adapter.save(text(otherParfaitId, otherGroupMemberId))

        adapter.findAllByParfaitId(parfaitId).map { it.id } shouldBe listOf(low.id, sameZ.id, high.id)
    }

    @Test
    fun `텍스트가 없는 파르페는 빈 목록과 false를 반환한다`() {
        val (parfaitId, _) = fixture("D")

        adapter.findAllByParfaitId(parfaitId) shouldBe emptyList()
        adapter.existsByParfaitId(parfaitId) shouldBe false
    }

    @Test
    fun `수정하면 같은 id로 갱신되고 배치자와 생성 시각은 유지된다`() {
        val (parfaitId, groupMemberId) = fixture("E")
        val saved = adapter.save(text(parfaitId, groupMemberId))
        val modifiedAt = saved.createdAt.plusHours(1)

        adapter.save(
            saved.update(
                positionX = 0.0,
                positionY = 10.0,
                positionZ = 5,
                scale = 2.0,
                rotation = 45.0,
                color = "#FFFFFF",
                fontSize = 24.0,
                isFill = true,
                isStroke = false,
                now = modifiedAt,
            ),
        )

        val found = requireNotNull(adapter.findById(saved.requireId()))
        found.text shouldBe saved.text
        found.positionX shouldBe 0.0
        found.positionY shouldBe 10.0
        found.positionZ shouldBe 5
        found.scale shouldBe 2.0
        found.rotation shouldBe 45.0
        found.color shouldBe "#FFFFFF"
        found.fontSize shouldBe 24.0
        found.isFill shouldBe true
        found.isStroke shouldBe false
        found.placedByGroupMemberId shouldBe saved.placedByGroupMemberId
        found.createdAt shouldBe saved.createdAt
        found.updatedAt shouldBe modifiedAt
        adapter.findAllByParfaitId(parfaitId).size shouldBe 1
    }

    @Test
    fun `삭제하면 지정한 텍스트만 조회에서 사라진다`() {
        val (parfaitId, groupMemberId) = fixture("F")
        val removed = adapter.save(text(parfaitId, groupMemberId))
        val retained = adapter.save(text(parfaitId, groupMemberId))

        adapter.deleteById(removed.requireId())

        adapter.findById(removed.requireId()) shouldBe null
        adapter.findAllByParfaitId(parfaitId).map { it.id } shouldBe listOf(retained.id)
        adapter.existsByParfaitId(parfaitId) shouldBe true

        adapter.deleteById(retained.requireId())

        adapter.findAllByParfaitId(parfaitId) shouldBe emptyList()
        adapter.existsByParfaitId(parfaitId) shouldBe false
    }

    @Test
    fun `존재하지 않는 텍스트 id는 null로 조회된다`() {
        adapter.findById(-1L) shouldBe null
    }
}
