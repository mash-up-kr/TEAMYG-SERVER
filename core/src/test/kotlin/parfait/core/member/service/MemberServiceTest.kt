package parfait.core.member.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.auth.domain.LoginProvider
import parfait.core.exception.BusinessException
import parfait.core.member.exception.MemberErrorCode
import parfait.core.member.port.`in`.ChangeGlobalNicknameResult
import parfait.core.member.port.`in`.MyAccountResult
import parfait.core.member.port.out.MemberAccount
import parfait.core.member.port.out.MemberNicknameUpdatePort
import parfait.core.member.port.out.MemberQueryPort
import kotlin.test.assertFailsWith

class MemberServiceTest {
    private val memberNicknameUpdatePort = mockk<MemberNicknameUpdatePort>(relaxed = true)
    private val memberQueryPort = mockk<MemberQueryPort>()
    private val service = MemberService(memberNicknameUpdatePort, memberQueryPort)

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
