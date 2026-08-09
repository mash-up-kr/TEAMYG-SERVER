package parfait.core.member.service

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.member.exception.MemberErrorCode
import parfait.core.member.port.`in`.ChangeGlobalNicknameResult
import parfait.core.member.port.out.MemberNicknameUpdatePort
import kotlin.test.assertFailsWith

class MemberServiceTest {
    private val memberNicknameUpdatePort = mockk<MemberNicknameUpdatePort>(relaxed = true)
    private val service = MemberService(memberNicknameUpdatePort)

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
}
