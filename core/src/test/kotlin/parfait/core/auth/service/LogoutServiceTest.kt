package parfait.core.auth.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.out.RefreshTokenClaims
import parfait.core.auth.port.out.TokenDeletePort
import parfait.core.auth.port.out.TokenValidatePort
import parfait.core.exception.BusinessException
import kotlin.test.assertFailsWith

class LogoutServiceTest {
    private val tokenValidatePort = mockk<TokenValidatePort>()
    private val tokenDeletePort = mockk<TokenDeletePort>(relaxed = true)
    private val service =
        LogoutService(
            tokenValidatePort = tokenValidatePort,
            tokenDeletePort = tokenDeletePort,
        )

    @Test
    fun `본인의 refreshToken이면 저장된 세션을 삭제한다`() {
        every { tokenValidatePort.validateRefreshToken("refresh-token") } returns
            RefreshTokenClaims(memberId = 42L, sessionId = "session-1")

        service.logout(memberId = 42L, refreshToken = "refresh-token")

        verify { tokenDeletePort.delete(42L, "session-1") }
    }

    @Test
    fun `다른 회원의 refreshToken이면 FORBIDDEN_REFRESH_TOKEN 예외를 던진다`() {
        every { tokenValidatePort.validateRefreshToken("other-token") } returns
            RefreshTokenClaims(memberId = 99L, sessionId = "session-1")

        val exception =
            assertFailsWith<BusinessException> {
                service.logout(memberId = 42L, refreshToken = "other-token")
            }

        exception.errorCode shouldBe AuthErrorCode.FORBIDDEN_REFRESH_TOKEN
        verify(exactly = 0) { tokenDeletePort.delete(any(), any()) }
    }

    @Test
    fun `refreshToken 검증에 실패하면 검증 예외를 그대로 전파한다`() {
        every { tokenValidatePort.validateRefreshToken("bad-token") } throws
            BusinessException(AuthErrorCode.INVALID_TOKEN)

        val exception =
            assertFailsWith<BusinessException> {
                service.logout(memberId = 42L, refreshToken = "bad-token")
            }

        exception.errorCode shouldBe AuthErrorCode.INVALID_TOKEN
    }
}
