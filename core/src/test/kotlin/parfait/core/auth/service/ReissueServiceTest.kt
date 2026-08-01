package parfait.core.auth.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.`in`.ReissueResult
import parfait.core.auth.port.out.RefreshTokenClaims
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.auth.port.out.TokenQueryPort
import parfait.core.auth.port.out.TokenSavePort
import parfait.core.auth.port.out.TokenValidatePort
import parfait.core.exception.BusinessException
import parfait.core.member.port.out.MemberQueryPort
import kotlin.test.assertFailsWith

class ReissueServiceTest {
    private val tokenValidatePort = mockk<TokenValidatePort>()
    private val tokenQueryPort = mockk<TokenQueryPort>()
    private val tokenIssuePort = mockk<TokenIssuePort>()
    private val tokenSavePort = mockk<TokenSavePort>(relaxed = true)
    private val memberQueryPort = mockk<MemberQueryPort>()
    private val service =
        ReissueService(
            tokenValidatePort = tokenValidatePort,
            tokenQueryPort = tokenQueryPort,
            tokenIssuePort = tokenIssuePort,
            tokenSavePort = tokenSavePort,
            memberQueryPort = memberQueryPort,
            accessTokenExpiresInSeconds = 3600,
            refreshTokenTtlSeconds = 1_209_600,
        )

    @Test
    fun `저장된 값과 일치하는 refreshToken이면 새 토큰을 발급하고 회전 저장한다`() {
        every { tokenValidatePort.validateRefreshToken("refresh-token") } returns
            RefreshTokenClaims(memberId = 42L, sessionId = "session-1")
        every { tokenQueryPort.findRefreshToken(42L, "session-1") } returns "refresh-token"
        every { memberQueryPort.existsById(42L) } returns true
        every { tokenIssuePort.createAccessToken(42L) } returns "new-access-token"
        every { tokenIssuePort.createRefreshToken(42L, "session-1") } returns "new-refresh-token"

        val result = service.reissue("refresh-token")

        result shouldBe ReissueResult("new-access-token", "new-refresh-token", 3600)
        verify { tokenSavePort.save(42L, "session-1", "new-refresh-token", 1_209_600) }
    }

    @Test
    fun `저장된 값이 없으면 INVALID_TOKEN 예외를 던진다`() {
        every { tokenValidatePort.validateRefreshToken("refresh-token") } returns
            RefreshTokenClaims(memberId = 42L, sessionId = "session-1")
        every { tokenQueryPort.findRefreshToken(42L, "session-1") } returns null

        val exception = assertFailsWith<BusinessException> { service.reissue("refresh-token") }

        exception.errorCode shouldBe AuthErrorCode.INVALID_TOKEN
    }

    @Test
    fun `저장된 값과 다른 refreshToken이면 INVALID_TOKEN 예외를 던진다`() {
        every { tokenValidatePort.validateRefreshToken("stolen-token") } returns
            RefreshTokenClaims(memberId = 42L, sessionId = "session-1")
        every { tokenQueryPort.findRefreshToken(42L, "session-1") } returns "current-token"

        val exception = assertFailsWith<BusinessException> { service.reissue("stolen-token") }

        exception.errorCode shouldBe AuthErrorCode.INVALID_TOKEN
    }

    @Test
    fun `존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외를 던진다`() {
        every { tokenValidatePort.validateRefreshToken("refresh-token") } returns
            RefreshTokenClaims(memberId = 42L, sessionId = "session-1")
        every { tokenQueryPort.findRefreshToken(42L, "session-1") } returns "refresh-token"
        every { memberQueryPort.existsById(42L) } returns false

        val exception = assertFailsWith<BusinessException> { service.reissue("refresh-token") }

        exception.errorCode shouldBe AuthErrorCode.MEMBER_NOT_FOUND
    }

    @Test
    fun `refreshToken 검증에 실패하면 검증 예외를 그대로 전파한다`() {
        every { tokenValidatePort.validateRefreshToken("bad-token") } throws
            BusinessException(AuthErrorCode.EXPIRED_TOKEN)

        val exception = assertFailsWith<BusinessException> { service.reissue("bad-token") }

        exception.errorCode shouldBe AuthErrorCode.EXPIRED_TOKEN
    }
}
