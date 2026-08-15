package parfait.core.parfaitgroup.application.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import parfait.core.member.port.out.MemberQueryPort
import parfait.core.parfait.port.`in`.EnsureActiveCanvasUseCase
import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameCommand
import parfait.core.parfaitgroup.application.port.`in`.CreateParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.JoinParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.ReportParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.out.MyParfaitGroupQueryPort
import parfait.core.parfaitgroup.application.port.out.MyParfaitGroupSummary
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberLeavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupReportSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupSavePort
import parfait.core.parfaitgroup.domain.InviteCode
import parfait.core.parfaitgroup.domain.InviteCodeGenerator
import parfait.core.parfaitgroup.domain.ParfaitGroup
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaitgroup.domain.ParfaitGroupReport
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class ParfaitGroupServiceTest {
    private val groupQueryPort = mockk<ParfaitGroupQueryPort>()
    private val groupSavePort = mockk<ParfaitGroupSavePort>()
    private val groupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val groupMemberSavePort = mockk<ParfaitGroupMemberSavePort>(relaxed = true)
    private val groupMemberLeavePort = mockk<ParfaitGroupMemberLeavePort>(relaxed = true)
    private val groupReportSavePort = mockk<ParfaitGroupReportSavePort>(relaxed = true)
    private val myGroupQueryPort = mockk<MyParfaitGroupQueryPort>(relaxed = true)
    private val memberQueryPort = mockk<MemberQueryPort>()
    private val inviteCodeGenerator = mockk<InviteCodeGenerator>()
    private val ensureActiveCanvasUseCase = mockk<EnsureActiveCanvasUseCase>(relaxed = true)
    private val service =
        ParfaitGroupService(
            parfaitGroupQueryPort = groupQueryPort,
            parfaitGroupSavePort = groupSavePort,
            parfaitGroupMemberQueryPort = groupMemberQueryPort,
            parfaitGroupMemberSavePort = groupMemberSavePort,
            parfaitGroupMemberLeavePort = groupMemberLeavePort,
            parfaitGroupReportSavePort = groupReportSavePort,
            myParfaitGroupQueryPort = myGroupQueryPort,
            memberQueryPort = memberQueryPort,
            inviteCodeGenerator = inviteCodeGenerator,
            ensureActiveCanvasUseCase = ensureActiveCanvasUseCase,
        )

    private val group = savedGroup(memberLimit = 2)

    @BeforeEach
    fun setUpJoinableGroup() {
        every { groupQueryPort.findByInviteCode(any()) } returns group
        every { groupQueryPort.findByInviteCodeForUpdate(any()) } returns group
        every { groupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns false
        every { groupMemberQueryPort.countByGroupId(1L) } returns 1
        every { memberQueryPort.findGlobalNicknameById(10L) } returns "멤버 닉네임"
        every { groupMemberQueryPort.existsByGroupIdAndNickname(1L, any()) } returns false
    }

    @Test
    fun `참여 미리보기는 검증에 성공하면 그룹명을 반환한다`() {
        val result = service.preview(memberId = 10L, inviteCode = "abcd12")

        result.groupName shouldBe "파르페"
        verify { groupQueryPort.findByInviteCode(InviteCode.of("ABCD12")) }
        verify(exactly = 0) { groupQueryPort.findByInviteCodeForUpdate(any()) }
    }

    @Test
    fun `존재하지 않는 초대코드는 미리보기에서 거부한다`() {
        every { groupQueryPort.findByInviteCode(any()) } returns null

        assertError(ParfaitGroupError.INVALID_INVITE_CODE) {
            service.preview(memberId = 10L, inviteCode = "NONE12")
        }
    }

    @Test
    fun `이미 참여한 그룹은 미리보기에서 거부한다`() {
        every { groupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true

        assertError(ParfaitGroupError.GROUP_ALREADY_JOINED) {
            service.preview(memberId = 10L, inviteCode = "ABCD12")
        }
    }

    @Test
    fun `최대 인원이 참여한 그룹은 미리보기에서 거부한다`() {
        every { groupMemberQueryPort.countByGroupId(1L) } returns 2

        assertError(ParfaitGroupError.GROUP_MEMBER_LIMIT_REACHED) {
            service.preview(memberId = 10L, inviteCode = "ABCD12")
        }
    }

    @Test
    fun `그룹 참여는 잠금 조회 후 전역 닉네임으로 멤버십을 저장한다`() {
        val memberSlot = slot<ParfaitGroupMember>()
        every { groupMemberSavePort.save(capture(memberSlot)) } answers { firstArg() }

        val result = service.join(JoinParfaitGroupCommand(memberId = 10L, inviteCode = "ABCD12"))

        result.groupId shouldBe 1L
        result.groupName shouldBe "파르페"
        memberSlot.captured.parfaitGroupId shouldBe 1L
        memberSlot.captured.memberId shouldBe 10L
        memberSlot.captured.groupNickname.value shouldBe "멤버 닉네임"
        verify { groupQueryPort.findByInviteCodeForUpdate(InviteCode.of("ABCD12")) }
    }

    @Test
    fun `같은 그룹에서 이미 사용 중인 전역 닉네임이면 참여를 거부한다`() {
        every { groupMemberQueryPort.existsByGroupIdAndNickname(1L, any()) } returns true

        assertError(ParfaitGroupError.GROUP_NICKNAME_ALREADY_USED) {
            service.join(JoinParfaitGroupCommand(memberId = 10L, inviteCode = "ABCD12"))
        }
    }

    @Test
    fun `그룹 생성은 자동 초대코드를 발급하고 생성자를 첫 멤버로 저장한다`() {
        val generatedCode = InviteCode.of("NEWC12")
        val memberSlot = slot<ParfaitGroupMember>()
        every { inviteCodeGenerator.generate() } returns generatedCode
        every { groupQueryPort.existsByInviteCode(generatedCode) } returns false
        every { memberQueryPort.existsById(10L) } returns true
        every { groupSavePort.save(any()) } answers {
            val requested = firstArg<ParfaitGroup>()
            ParfaitGroup.reconstitute(
                id = 20L,
                name = requested.name.value,
                inviteCode = requested.inviteCode.value,
                memberLimit = requested.memberLimit.value,
                createdAt = requested.createdAt,
                updatedAt = requested.updatedAt,
            )
        }
        every { groupMemberSavePort.save(capture(memberSlot)) } answers { firstArg() }

        val result =
            service.create(
                CreateParfaitGroupCommand(
                    memberId = 10L,
                    groupName = "새 그룹",
                    groupNickname = "그룹 닉네임",
                    memberLimit = 12,
                ),
            )

        result.groupId shouldBe 20L
        result.groupName shouldBe "새 그룹"
        result.inviteCode shouldBe "NEWC12"
        result.memberLimit shouldBe 12
        memberSlot.captured.parfaitGroupId shouldBe 20L
        memberSlot.captured.memberId shouldBe 10L
        memberSlot.captured.groupNickname.value shouldBe "그룹 닉네임"
    }

    @Test
    fun `그룹을 생성하면 오늘 날짜의 활성 캔버스를 즉시 생성한다`() {
        every { inviteCodeGenerator.generate() } returns InviteCode.of("NEWC12")
        every { groupQueryPort.existsByInviteCode(any()) } returns false
        every { memberQueryPort.existsById(10L) } returns true
        every { groupSavePort.save(any()) } answers {
            val requested = firstArg<ParfaitGroup>()
            ParfaitGroup.reconstitute(
                id = 20L,
                name = requested.name.value,
                inviteCode = requested.inviteCode.value,
                memberLimit = requested.memberLimit.value,
                createdAt = requested.createdAt,
                updatedAt = requested.updatedAt,
            )
        }
        every { groupMemberSavePort.save(any()) } answers { firstArg() }

        service.create(
            CreateParfaitGroupCommand(
                memberId = 10L,
                groupName = "새 그룹",
                groupNickname = "그룹 닉네임",
                memberLimit = 12,
            ),
        )

        verify { ensureActiveCanvasUseCase.ensure(20L, any()) }
    }

    @Test
    fun `내 그룹 목록은 최근 활동 순으로 조회 포트의 결과를 그대로 반환한다`() {
        every { myGroupQueryPort.findAllByMemberId(10L) } returns
            listOf(
                MyParfaitGroupSummary(2L, "최근 그룹", "https://image.example/2", LocalDateTime.MAX),
                MyParfaitGroupSummary(1L, "이전 그룹", null, null),
            )

        val result = service.getAll(10L)

        result.map { it.groupId } shouldBe listOf(2L, 1L)
        result.first().recentImageUrl shouldBe "https://image.example/2"
        result.first().recentImageUploadedAt shouldBe LocalDateTime.MAX
    }

    @Test
    fun `내 그룹 상세는 내 닉네임과 전체 그룹원 및 초대코드를 반환한다`() {
        val myMembership = membership(memberId = 10L, nickname = "내 닉네임")
        val otherMembership = membership(memberId = 20L, nickname = "그룹원")
        every { groupQueryPort.findById(1L) } returns group
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 10L) } returns myMembership
        every { groupMemberQueryPort.findAllByGroupId(1L) } returns listOf(myMembership, otherMembership)

        val result = service.get(memberId = 10L, groupId = 1L)

        result.groupId shouldBe 1L
        result.groupNickname shouldBe "내 닉네임"
        result.inviteCode shouldBe "ABCD12"
        result.members.map { it.memberId } shouldBe listOf(10L, 20L)
    }

    @Test
    fun `그룹 닉네임 변경은 그룹을 잠그고 중복이 없으면 멤버십을 저장한다`() {
        val currentMembership = membership(memberId = 10L, nickname = "기존 닉네임")
        val savedSlot = slot<ParfaitGroupMember>()
        every { groupQueryPort.findByIdForUpdate(1L) } returns group
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 10L) } returns currentMembership
        every { groupMemberQueryPort.existsByGroupIdAndNickname(1L, any()) } returns false
        every { groupMemberSavePort.save(capture(savedSlot)) } answers { firstArg() }

        val result =
            service.change(
                ChangeMyParfaitGroupNicknameCommand(
                    memberId = 10L,
                    groupId = 1L,
                    groupNickname = "새 닉네임",
                ),
            )

        result.groupNickname shouldBe "새 닉네임"
        savedSlot.captured.groupNickname.value shouldBe "새 닉네임"
        verify { groupQueryPort.findByIdForUpdate(1L) }
    }

    @Test
    fun `이미 사용 중인 그룹 닉네임으로 변경할 수 없다`() {
        every { groupQueryPort.findByIdForUpdate(1L) } returns group
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 10L) } returns membership(10L, "기존 닉네임")
        every { groupMemberQueryPort.existsByGroupIdAndNickname(1L, any()) } returns true

        assertError(ParfaitGroupError.GROUP_NICKNAME_ALREADY_USED) {
            service.change(ChangeMyParfaitGroupNicknameCommand(10L, 1L, "다른 닉네임"))
        }
        verify(exactly = 0) { groupMemberSavePort.save(any()) }
    }

    @Test
    fun `그룹 나가기는 알수없음 닉네임의 탈퇴 이력으로 멤버십을 남긴다`() {
        val membership = membership(memberId = 10L, nickname = "내 닉네임")
        val leftMemberSlot = slot<ParfaitGroupMember>()
        every { groupQueryPort.findByIdForUpdate(1L) } returns group
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 10L) } returns membership
        every { groupMemberLeavePort.leave(capture(leftMemberSlot)) } returns Unit

        val result = service.leave(LeaveParfaitGroupCommand(memberId = 10L, groupId = 1L))

        result.groupId shouldBe 1L
        leftMemberSlot.captured.groupNickname.value shouldBe "(알수없음)"
        (leftMemberSlot.captured.leftAt != null) shouldBe true
    }

    @Test
    fun `그룹 신고는 신고를 저장한 뒤 같은 트랜잭션에서 탈퇴 이력을 남긴다`() {
        val membership = membership(memberId = 10L, nickname = "내 닉네임")
        val leftMemberSlot = slot<ParfaitGroupMember>()
        every { groupQueryPort.findByIdForUpdate(1L) } returns group
        every { groupMemberQueryPort.findByGroupIdAndMemberId(1L, 10L) } returns membership
        every { groupMemberLeavePort.leave(capture(leftMemberSlot)) } returns Unit
        every { groupReportSavePort.save(any()) } answers {
            val report = firstArg<ParfaitGroupReport>()
            ParfaitGroupReport.reconstitute(
                id = 99L,
                parfaitGroupId = report.parfaitGroupId,
                reporterMemberId = report.reporterMemberId,
                reason = report.reason,
                createdAt = report.createdAt,
            )
        }

        val result = service.report(ReportParfaitGroupCommand(10L, 1L, "부적절한 그룹입니다"))

        result.groupId shouldBe 1L
        result.reportId shouldBe 99L
        verifySequence {
            groupReportSavePort.save(any())
            groupMemberLeavePort.leave(any())
        }
        leftMemberSlot.captured.groupNickname.value shouldBe "(알수없음)"
        (leftMemberSlot.captured.leftAt != null) shouldBe true
    }

    @Test
    fun `비어 있는 신고 사유는 신고나 탈퇴 없이 거부한다`() {
        assertError(ParfaitGroupError.INVALID_GROUP_REPORT_REASON) {
            service.report(ReportParfaitGroupCommand(10L, 1L, "  "))
        }
        verify(exactly = 0) { groupMemberLeavePort.leave(any()) }
        verify(exactly = 0) { groupReportSavePort.save(any()) }
    }

    private fun savedGroup(memberLimit: Int): ParfaitGroup =
        ParfaitGroup.reconstitute(
            id = 1L,
            name = "파르페",
            inviteCode = "ABCD12",
            memberLimit = memberLimit,
            createdAt = LocalDateTime.MIN,
            updatedAt = LocalDateTime.MIN,
        )

    private fun membership(
        memberId: Long,
        nickname: String,
    ): ParfaitGroupMember =
        ParfaitGroupMember.reconstitute(
            id = memberId + 100L,
            parfaitGroupId = 1L,
            memberId = memberId,
            groupNickname = nickname,
            joinedAt = LocalDateTime.MIN,
        )

    private fun assertError(
        expected: ParfaitGroupError,
        block: () -> Unit,
    ) {
        assertFailsWith<ParfaitGroupException>(block = block).error shouldBe expected
    }
}
