package parfait.core.auth.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.port.`in`.AppleLoginResult
import parfait.core.auth.port.out.AppleAuthorizationCodeExchangePort
import parfait.core.auth.port.out.AppleIdTokenVerifyPort
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.auth.port.out.TokenSavePort
import parfait.core.member.port.out.MemberAppleRefreshTokenSavePort
import parfait.core.member.port.out.MemberQueryPort

class AppleLoginServiceTest {
    private val appleIdTokenVerifyPort = mockk<AppleIdTokenVerifyPort>()
    private val appleAuthorizationCodeExchangePort = mockk<AppleAuthorizationCodeExchangePort>()
    private val memberQueryPort = mockk<MemberQueryPort>()
    private val memberAppleRefreshTokenSavePort = mockk<MemberAppleRefreshTokenSavePort>(relaxed = true)
    private val tokenIssuePort = mockk<TokenIssuePort>()
    private val tokenSavePort = mockk<TokenSavePort>(relaxed = true)
    private val service =
        AppleLoginService(
            appleIdTokenVerifyPort = appleIdTokenVerifyPort,
            appleAuthorizationCodeExchangePort = appleAuthorizationCodeExchangePort,
            memberQueryPort = memberQueryPort,
            memberAppleRefreshTokenSavePort = memberAppleRefreshTokenSavePort,
            tokenIssuePort = tokenIssuePort,
            tokenSavePort = tokenSavePort,
            accessTokenExpiresInSeconds = 3600,
            refreshTokenTtlSeconds = 1_209_600,
        )

    @Test
    fun `기존 회원이면 애플 refreshToken을 갱신하고 액세스-리프레시 토큰을 발급한다`() {
        every { appleIdTokenVerifyPort.verify("id-token", "nonce-1") } returns "apple-sub-1"
        every { appleAuthorizationCodeExchangePort.exchange("auth-code-1") } returns "apple-refresh-1"
        every { memberQueryPort.findMemberIdByProvider(LoginProvider.APPLE, "apple-sub-1") } returns 42L
        every { tokenIssuePort.createAccessToken(42L) } returns "access-token"
        every { tokenIssuePort.createRefreshToken(42L, any()) } returns "refresh-token"

        val result = service.login("id-token", "nonce-1", "auth-code-1")

        result shouldBe AppleLoginResult.ExistingMember("access-token", "refresh-token", 3600)
        verify { memberAppleRefreshTokenSavePort.saveRefreshToken(42L, "apple-refresh-1") }
        verify { tokenSavePort.save(42L, any(), "refresh-token", 1_209_600) }
    }

    @Test
    fun `신규 유저면 appleRefreshToken을 담은 가입용 토큰을 발급한다`() {
        every { appleIdTokenVerifyPort.verify("id-token", "nonce-2") } returns "apple-sub-2"
        every { appleAuthorizationCodeExchangePort.exchange("auth-code-2") } returns "apple-refresh-2"
        every { memberQueryPort.findMemberIdByProvider(LoginProvider.APPLE, "apple-sub-2") } returns null
        every {
            tokenIssuePort.createRegistrationToken(LoginProvider.APPLE, "apple-sub-2", "apple-refresh-2")
        } returns "registration-token"

        val result = service.login("id-token", "nonce-2", "auth-code-2")

        result shouldBe AppleLoginResult.NewUser("registration-token")
        verify(exactly = 0) { memberAppleRefreshTokenSavePort.saveRefreshToken(any(), any()) }
        verify(exactly = 0) { tokenSavePort.save(any(), any(), any(), any()) }
    }
}
