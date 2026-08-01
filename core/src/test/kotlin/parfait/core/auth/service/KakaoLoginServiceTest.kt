package parfait.core.auth.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.port.`in`.KakaoLoginResult
import parfait.core.auth.port.out.KakaoIdTokenVerifyPort
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.auth.port.out.TokenSavePort
import parfait.core.member.port.out.MemberQueryPort

class KakaoLoginServiceTest {
    private val kakaoIdTokenVerifyPort = mockk<KakaoIdTokenVerifyPort>()
    private val memberQueryPort = mockk<MemberQueryPort>()
    private val tokenIssuePort = mockk<TokenIssuePort>()
    private val tokenSavePort = mockk<TokenSavePort>(relaxed = true)
    private val service =
        KakaoLoginService(
            kakaoIdTokenVerifyPort = kakaoIdTokenVerifyPort,
            memberQueryPort = memberQueryPort,
            tokenIssuePort = tokenIssuePort,
            tokenSavePort = tokenSavePort,
            accessTokenExpiresInSeconds = 3600,
            refreshTokenTtlSeconds = 1_209_600,
        )

    @Test
    fun `기존 회원이면 액세스-리프레시 토큰을 발급하고 저장한 뒤 ExistingMember를 반환한다`() {
        every { kakaoIdTokenVerifyPort.verify("id-token", "nonce-1") } returns "kakao-sub-1"
        every { memberQueryPort.findMemberIdByProvider(LoginProvider.KAKAO, "kakao-sub-1") } returns 42L
        every { tokenIssuePort.createAccessToken(42L) } returns "access-token"
        every { tokenIssuePort.createRefreshToken(42L, any()) } returns "refresh-token"

        val result = service.login("id-token", "nonce-1")

        result shouldBe KakaoLoginResult.ExistingMember("access-token", "refresh-token", 3600)
        verify { tokenSavePort.save(42L, any(), "refresh-token", 1_209_600) }
    }

    @Test
    fun `신규 유저면 가입용 토큰을 발급하고 NewUser를 반환한다`() {
        every { kakaoIdTokenVerifyPort.verify("id-token", "nonce-2") } returns "kakao-sub-2"
        every { memberQueryPort.findMemberIdByProvider(LoginProvider.KAKAO, "kakao-sub-2") } returns null
        every {
            tokenIssuePort.createRegistrationToken(LoginProvider.KAKAO, "kakao-sub-2")
        } returns "registration-token"

        val result = service.login("id-token", "nonce-2")

        result shouldBe KakaoLoginResult.NewUser("registration-token")
        verify(exactly = 0) { tokenSavePort.save(any(), any(), any(), any()) }
    }
}
