package parfait.persistence.adapter

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.parfaitgroup.application.port.out.MyParfaitGroupSummary
import parfait.core.parfaitgroup.domain.GroupNickname
import parfait.core.parfaitgroup.domain.InviteCode
import parfait.core.parfaitgroup.domain.ParfaitGroup
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.persistence.repository.MyParfaitGroupSummaryProjection
import parfait.persistence.repository.ParfaitGroupMemberRepository
import parfait.persistence.repository.ParfaitGroupRepository
import java.time.LocalDateTime
import parfait.persistence.entity.ParfaitGroup as ParfaitGroupEntity
import parfait.persistence.entity.ParfaitGroupMember as ParfaitGroupMemberEntity

class ParfaitGroupAdapterTest {
    private val groupRepository = mockk<ParfaitGroupRepository>()
    private val groupMemberRepository = mockk<ParfaitGroupMemberRepository>()
    private val adapter = ParfaitGroupAdapter(groupRepository, groupMemberRepository)
    private val now = LocalDateTime.of(2026, 8, 1, 12, 0)

    @Test
    fun `초대코드로 조회한 JPA 엔티티를 도메인 그룹으로 변환한다`() {
        every { groupRepository.findByInviteCode("ABCD1234") } returns groupEntity()

        val result = adapter.findByInviteCode(InviteCode.of("ABCD1234"))!!

        result.id shouldBe 1L
        result.name.value shouldBe "우리 그룹"
        result.inviteCode.value shouldBe "ABCD1234"
        result.memberLimit.value shouldBe 12
    }

    @Test
    fun `잠금 조회는 별도의 JPA 잠금 쿼리를 사용한다`() {
        every { groupRepository.findByInviteCodeForUpdate("ABCD1234") } returns groupEntity()

        adapter.findByInviteCodeForUpdate(InviteCode.of("ABCD1234"))

        verify { groupRepository.findByInviteCodeForUpdate("ABCD1234") }
    }

    @Test
    fun `그룹 id 잠금 조회는 별도의 JPA 잠금 쿼리를 사용한다`() {
        every { groupRepository.findByIdForUpdate(1L) } returns groupEntity()

        adapter.findByIdForUpdate(1L)

        verify { groupRepository.findByIdForUpdate(1L) }
    }

    @Test
    fun `도메인 그룹을 JPA 엔티티로 저장하고 다시 도메인으로 반환한다`() {
        val group =
            ParfaitGroup.create(
                name = "우리 그룹",
                inviteCode = InviteCode.of("ABCD1234"),
                memberLimit = 12,
                now = now,
            )
        every { groupRepository.save(any()) } answers {
            firstArg<ParfaitGroupEntity>().apply { id = 1L }
        }

        val saved = adapter.save(group)

        saved.id shouldBe 1L
        saved.name.value shouldBe "우리 그룹"
    }

    @Test
    fun `멤버십 조회와 저장을 그룹 멤버 리포지토리에 위임한다`() {
        val member = ParfaitGroupMember.join(1L, 10L, "내 닉네임", now)
        every { groupMemberRepository.existsByParfaitGroupIdAndMemberId(1L, 10L) } returns true
        every { groupMemberRepository.existsByParfaitGroupIdAndGroupNicknameAndLeftAtIsNull(1L, "내 닉네임") } returns true
        every { groupMemberRepository.countByParfaitGroupIdAndLeftAtIsNull(1L) } returns 3L
        every { groupMemberRepository.save(any()) } answers {
            firstArg<ParfaitGroupMemberEntity>().apply { id = 2L }
        }

        adapter.existsByGroupIdAndMemberId(1L, 10L) shouldBe true
        adapter.existsByGroupIdAndNickname(1L, GroupNickname.of("내 닉네임")) shouldBe true
        adapter.countByGroupId(1L) shouldBe 3
        adapter.save(member).id shouldBe 2L
    }

    @Test
    fun `내 그룹 목록 조회 projection을 core 조회 모델로 변환한다`() {
        val projection = mockk<MyParfaitGroupSummaryProjection>()
        every { projection.groupId } returns 1L
        every { projection.groupName } returns "우리 그룹"
        every { projection.recentImageUrl } returns "https://image.example/latest"
        every { projection.recentImageUploadedAt } returns now
        every { groupMemberRepository.findMyGroupSummaries(10L) } returns listOf(projection)

        adapter.findAllByMemberId(10L) shouldBe
            listOf(
                MyParfaitGroupSummary(
                    groupId = 1L,
                    groupName = "우리 그룹",
                    recentImageUrl = "https://image.example/latest",
                    recentImageUploadedAt = now,
                ),
            )
    }

    @Test
    fun `멤버십 단건 조회와 탈퇴 이력 저장은 JPA 엔티티를 노출하지 않는다`() {
        val entity =
            ParfaitGroupMemberEntity(
                parfaitGroupId = 1L,
                memberId = 10L,
                groupNickname = "내 닉네임",
                joinedAt = now,
                id = 2L,
            )
        every { groupMemberRepository.findByParfaitGroupIdAndMemberIdAndLeftAtIsNull(1L, 10L) } returns entity
        every { groupMemberRepository.save(any()) } answers { firstArg<ParfaitGroupMemberEntity>() }

        val membership = adapter.findByGroupIdAndMemberId(1L, 10L)!!
        adapter.leave(membership.leave(now))

        membership.groupNickname.value shouldBe "내 닉네임"
        verify {
            groupMemberRepository.save(
                match { it.id == 2L && it.groupNickname == "(알수없음)" && it.leftAt == now },
            )
        }
    }

    @Test
    fun `회원의 활성 그룹 멤버십 전체를 도메인으로 변환해 반환한다`() {
        val entity =
            ParfaitGroupMemberEntity(
                parfaitGroupId = 1L,
                memberId = 10L,
                groupNickname = "내 닉네임",
                joinedAt = now,
                id = 2L,
            )
        every {
            groupMemberRepository.findAllByMemberIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(10L)
        } returns listOf(entity)

        val result = adapter.findAllMembershipsByMemberId(10L)

        result.single().id shouldBe 2L
        result.single().parfaitGroupId shouldBe 1L
    }

    private fun groupEntity(): ParfaitGroupEntity =
        ParfaitGroupEntity(
            name = "우리 그룹",
            inviteCode = "ABCD1234",
            memberLimit = 12,
            createdAt = now,
            updatedAt = now,
            id = 1L,
        )
}
