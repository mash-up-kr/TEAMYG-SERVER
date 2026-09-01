package parfait.core.member.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronizationManager
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.port.out.TokenDeletePort
import parfait.core.exception.BusinessException
import parfait.core.member.exception.MemberErrorCode
import parfait.core.member.port.`in`.ChangeGlobalNicknameResult
import parfait.core.member.port.`in`.MyAccountResult
import parfait.core.member.port.out.MemberAccount
import parfait.core.member.port.out.MemberDeletePort
import parfait.core.member.port.out.MemberNicknameUpdatePort
import parfait.core.member.port.out.MemberQueryPort
import parfait.core.notification.port.out.DeviceTokenDeletePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberLeavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class MemberServiceTest {
    private val memberNicknameUpdatePort = mockk<MemberNicknameUpdatePort>(relaxed = true)
    private val memberQueryPort = mockk<MemberQueryPort>()
    private val memberDeletePort = mockk<MemberDeletePort>(relaxed = true)
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitGroupMemberLeavePort = mockk<ParfaitGroupMemberLeavePort>(relaxed = true)
    private val tokenDeletePort = mockk<TokenDeletePort>(relaxed = true)
    private val deviceTokenDeletePort = mockk<DeviceTokenDeletePort>(relaxed = true)
    private val service =
        MemberService(
            memberNicknameUpdatePort = memberNicknameUpdatePort,
            memberQueryPort = memberQueryPort,
            memberDeletePort = memberDeletePort,
            parfaitGroupMemberQueryPort = parfaitGroupMemberQueryPort,
            parfaitGroupMemberLeavePort = parfaitGroupMemberLeavePort,
            tokenDeletePort = tokenDeletePort,
            deviceTokenDeletePort = deviceTokenDeletePort,
        )
    private val now = LocalDateTime.of(2026, 8, 11, 12, 0)

    // withdraw()가 실제 @Transactional 프록시 없이 호출되므로, 커밋 후 콜백을 직접 시뮬레이션한다.
    @BeforeEach
    fun setUpTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization()
    }

    @AfterEach
    fun tearDownTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization()
    }

    private fun triggerAfterCommit() {
        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
    }

    @Test
    fun `형식이 올바른 닉네임은 저장하고 결과로 반환한다`() {
        val result = service.change(42L, "부지런한 수달")

        result shouldBe ChangeGlobalNicknameResult("부지런한 수달")
        verify { memberNicknameUpdatePort.updateGlobalNickname(42L, "부지런한 수달") }
    }

    @Test
    fun `형식이 잘못된 닉네임은 INVALID_NICKNAME 예외를 던지고 저장을 시도하지 않는다`() {
        val exception =
            assertFailsWith<BusinessException> {
                service.change(42L, "연속  공백")
            }

        exception.errorCode shouldBe MemberErrorCode.INVALID_NICKNAME
        verify(exactly = 0) { memberNicknameUpdatePort.updateGlobalNickname(any(), any()) }
    }

    @Test
    fun `탈퇴하면 회원을 삭제하고 그룹을 탈퇴시키고, 트랜잭션 커밋 후 refresh token을 정리한다`() {
        every { memberQueryPort.existsById(42L) } returns true
        val membership = ParfaitGroupMember.reconstitute(1L, 100L, 42L, "내 닉네임", now)
        every { parfaitGroupMemberQueryPort.findAllMembershipsByMemberId(42L) } returns listOf(membership)

        service.withdraw(42L)

        verify { memberDeletePort.deleteById(42L) }
        verify { parfaitGroupMemberLeavePort.leave(match { it.parfaitGroupId == 100L && it.leftAt != null }) }
        verify(exactly = 0) { tokenDeletePort.deleteAllByMemberId(any()) }
        verify(exactly = 0) { deviceTokenDeletePort.deleteAllByMemberId(any()) }

        triggerAfterCommit()

        verify { tokenDeletePort.deleteAllByMemberId(42L) }
        verify { deviceTokenDeletePort.deleteAllByMemberId(42L) }
    }

    @Test
    fun `여러 그룹에 가입되어 있으면 모든 멤버십을 탈퇴 처리한다`() {
        every { memberQueryPort.existsById(45L) } returns true
        val membershipA = ParfaitGroupMember.reconstitute(1L, 100L, 45L, "닉A", now)
        val membershipB = ParfaitGroupMember.reconstitute(2L, 200L, 45L, "닉B", now)
        every { parfaitGroupMemberQueryPort.findAllMembershipsByMemberId(45L) } returns
            listOf(membershipA, membershipB)

        service.withdraw(45L)

        verify { parfaitGroupMemberLeavePort.leave(match { it.parfaitGroupId == 100L }) }
        verify { parfaitGroupMemberLeavePort.leave(match { it.parfaitGroupId == 200L }) }
    }

    @Test
    fun `이미 탈퇴된 회원이면 아무 것도 하지 않고 정상 종료한다`() {
        every { memberQueryPort.existsById(46L) } returns false

        service.withdraw(46L)

        verify(exactly = 0) { memberDeletePort.deleteById(any()) }
        verify(exactly = 0) { parfaitGroupMemberQueryPort.findAllMembershipsByMemberId(any()) }
        verify(exactly = 0) { tokenDeletePort.deleteAllByMemberId(any()) }
        verify(exactly = 0) { deviceTokenDeletePort.deleteAllByMemberId(any()) }
    }

    @Test
    fun `커밋 후 refresh token 정리가 실패해도 예외를 던지지 않는다`() {
        every { memberQueryPort.existsById(47L) } returns true
        every { parfaitGroupMemberQueryPort.findAllMembershipsByMemberId(47L) } returns emptyList()
        every { tokenDeletePort.deleteAllByMemberId(47L) } throws RuntimeException("redis down")

        service.withdraw(47L)
        triggerAfterCommit()

        verify { memberDeletePort.deleteById(47L) }
    }

    @Test
    fun `커밋 후 기기 토큰 정리가 실패해도 예외를 던지지 않는다`() {
        every { memberQueryPort.existsById(48L) } returns true
        every { parfaitGroupMemberQueryPort.findAllMembershipsByMemberId(48L) } returns emptyList()
        every { deviceTokenDeletePort.deleteAllByMemberId(48L) } throws RuntimeException("device token delete failed")

        service.withdraw(48L)
        triggerAfterCommit()

        verify { memberDeletePort.deleteById(48L) }
    }

    @Test
    fun `회원 계정 정보를 조회하면 provider와 닉네임을 포함한 결과를 반환한다`() {
        every { memberQueryPort.findAccountById(42L) } returns MemberAccount(LoginProvider.KAKAO, "행복한 판다")

        val result = service.getMyAccount(42L)

        result shouldBe MyAccountResult(42L, LoginProvider.KAKAO, "행복한 판다")
    }

    @Test
    fun `존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외를 던진다`() {
        every { memberQueryPort.findAccountById(999L) } returns null

        val exception =
            assertFailsWith<BusinessException> {
                service.getMyAccount(999L)
            }

        exception.errorCode shouldBe MemberErrorCode.MEMBER_NOT_FOUND
    }
}
