package parfait.core.auth.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.domain.TosType
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.`in`.SignupResult
import parfait.core.auth.port.`in`.TermsAgreement
import parfait.core.auth.port.out.CurrentTerms
import parfait.core.auth.port.out.RegistrationTokenClaims
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.auth.port.out.TokenSavePort
import parfait.core.auth.port.out.TokenValidatePort
import parfait.core.auth.port.out.TosQueryPort
import parfait.core.exception.BusinessException
import parfait.core.member.domain.RandomNicknameGenerator
import parfait.core.member.port.out.MemberAppleRefreshTokenSavePort
import parfait.core.member.port.out.MemberQueryPort
import kotlin.test.assertFailsWith

class SignupServiceTest {
    private val tokenValidatePort = mockk<TokenValidatePort>()
    private val memberQueryPort = mockk<MemberQueryPort>()
    private val tosQueryPort = mockk<TosQueryPort>()
    private val memberRegistrar = mockk<MemberRegistrar>()
    private val memberAppleRefreshTokenSavePort = mockk<MemberAppleRefreshTokenSavePort>(relaxed = true)
    private val tokenIssuePort = mockk<TokenIssuePort>()
    private val tokenSavePort = mockk<TokenSavePort>(relaxed = true)
    private val nicknameGenerator = mockk<RandomNicknameGenerator>()
    private val service =
        SignupService(
            tokenValidatePort = tokenValidatePort,
            memberQueryPort = memberQueryPort,
            tosQueryPort = tosQueryPort,
            memberRegistrar = memberRegistrar,
            memberAppleRefreshTokenSavePort = memberAppleRefreshTokenSavePort,
            tokenIssuePort = tokenIssuePort,
            tokenSavePort = tokenSavePort,
            nicknameGenerator = nicknameGenerator,
            accessTokenExpiresInSeconds = 3600,
            refreshTokenTtlSeconds = 1_209_600,
        )

    private val requiredTerms =
        CurrentTerms(
            id = 1L,
            type = TosType.TERMS_OF_SERVICE,
            title = "서비스 이용약관",
            url = "https://parfait.app/policies/terms",
            required = true,
        )
    private val optionalTerms =
        CurrentTerms(
            id = 2L,
            type = TosType.PRIVACY_POLICY,
            title = "개인정보 처리방침",
            url = "https://parfait.app/policies/privacy",
            required = false,
        )

    private fun stubHappyPath() {
        every { tokenValidatePort.validateRegistrationToken("reg-token") } returns
            RegistrationTokenClaims(LoginProvider.KAKAO, "kakao-sub-1")
        every { memberQueryPort.findMemberIdByProvider(LoginProvider.KAKAO, "kakao-sub-1") } returns null
        every { nicknameGenerator.generate() } returns "달콤한푸딩"
        every { memberRegistrar.register(any(), any(), any(), any()) } returns 42L
        every { tokenIssuePort.createAccessToken(42L) } returns "access-token"
        every { tokenIssuePort.createRefreshToken(42L, any()) } returns "refresh-token"
    }

    @Test
    fun `필수 약관에 동의하고 선택 약관은 생략하면 회원가입에 성공한다`() {
        every { tosQueryPort.findCurrentTerms() } returns listOf(requiredTerms, optionalTerms)
        stubHappyPath()

        val result = service.signup("reg-token", listOf(TermsAgreement(1L, true)))

        result shouldBe SignupResult("access-token", "refresh-token", 3600)
        verify { memberRegistrar.register(LoginProvider.KAKAO, "kakao-sub-1", "달콤한푸딩", listOf(1L)) }
        verify { tokenSavePort.save(42L, any(), "refresh-token", 1_209_600) }
    }

    @Test
    fun `선택 약관을 agreed true로 함께 보내면 같이 저장된다`() {
        every { tosQueryPort.findCurrentTerms() } returns listOf(requiredTerms, optionalTerms)
        stubHappyPath()

        service.signup("reg-token", listOf(TermsAgreement(1L, true), TermsAgreement(2L, true)))

        verify { memberRegistrar.register(LoginProvider.KAKAO, "kakao-sub-1", "달콤한푸딩", listOf(1L, 2L)) }
    }

    @Test
    fun `선택 약관을 agreed false로 보내면 저장되지 않는다`() {
        every { tosQueryPort.findCurrentTerms() } returns listOf(requiredTerms, optionalTerms)
        stubHappyPath()

        service.signup("reg-token", listOf(TermsAgreement(1L, true), TermsAgreement(2L, false)))

        verify { memberRegistrar.register(LoginProvider.KAKAO, "kakao-sub-1", "달콤한푸딩", listOf(1L)) }
    }

    @Test
    fun `termsId가 중복되면 DUPLICATE_TERMS_ID 예외를 던진다`() {
        every { tokenValidatePort.validateRegistrationToken("reg-token") } returns
            RegistrationTokenClaims(LoginProvider.KAKAO, "kakao-sub-1")
        every { tosQueryPort.findCurrentTerms() } returns listOf(requiredTerms)

        val exception =
            assertFailsWith<BusinessException> {
                service.signup("reg-token", listOf(TermsAgreement(1L, true), TermsAgreement(1L, true)))
            }
        exception.errorCode shouldBe AuthErrorCode.DUPLICATE_TERMS_ID
    }

    @Test
    fun `존재하지 않는 termsId면 TERMS_NOT_FOUND 예외를 던진다`() {
        every { tokenValidatePort.validateRegistrationToken("reg-token") } returns
            RegistrationTokenClaims(LoginProvider.KAKAO, "kakao-sub-1")
        every { tosQueryPort.findCurrentTerms() } returns listOf(requiredTerms)

        val exception =
            assertFailsWith<BusinessException> {
                service.signup("reg-token", listOf(TermsAgreement(999L, true)))
            }
        exception.errorCode shouldBe AuthErrorCode.TERMS_NOT_FOUND
    }

    @Test
    fun `필수 약관에 동의하지 않으면 REQUIRED_TERMS_NOT_AGREED 예외를 던진다`() {
        every { tokenValidatePort.validateRegistrationToken("reg-token") } returns
            RegistrationTokenClaims(LoginProvider.KAKAO, "kakao-sub-1")
        every { tosQueryPort.findCurrentTerms() } returns listOf(requiredTerms)

        val exception =
            assertFailsWith<BusinessException> {
                service.signup("reg-token", listOf(TermsAgreement(1L, false)))
            }
        exception.errorCode shouldBe AuthErrorCode.REQUIRED_TERMS_NOT_AGREED
    }

    @Test
    fun `필수 약관이 요청에 아예 없으면 REQUIRED_TERMS_NOT_AGREED 예외를 던진다`() {
        every { tokenValidatePort.validateRegistrationToken("reg-token") } returns
            RegistrationTokenClaims(LoginProvider.KAKAO, "kakao-sub-1")
        every { tosQueryPort.findCurrentTerms() } returns listOf(requiredTerms)

        val exception =
            assertFailsWith<BusinessException> {
                service.signup("reg-token", emptyList())
            }
        exception.errorCode shouldBe AuthErrorCode.REQUIRED_TERMS_NOT_AGREED
    }

    @Test
    fun `이미 가입된 회원이면 ALREADY_REGISTERED 예외를 던진다`() {
        every { tokenValidatePort.validateRegistrationToken("reg-token") } returns
            RegistrationTokenClaims(LoginProvider.KAKAO, "kakao-sub-1")
        every { tosQueryPort.findCurrentTerms() } returns listOf(requiredTerms)
        every { memberQueryPort.findMemberIdByProvider(LoginProvider.KAKAO, "kakao-sub-1") } returns 7L

        val exception =
            assertFailsWith<BusinessException> {
                service.signup("reg-token", listOf(TermsAgreement(1L, true)))
            }
        exception.errorCode shouldBe AuthErrorCode.ALREADY_REGISTERED
    }

    @Test
    fun `registrationToken 검증에 실패하면 검증 예외를 그대로 전파한다`() {
        every { tokenValidatePort.validateRegistrationToken("bad-token") } throws
            BusinessException(AuthErrorCode.EXPIRED_TOKEN)

        val exception =
            assertFailsWith<BusinessException> {
                service.signup("bad-token", listOf(TermsAgreement(1L, true)))
            }
        exception.errorCode shouldBe AuthErrorCode.EXPIRED_TOKEN
    }

    @Test
    fun `애플 회원가입이면 registrationToken의 appleRefreshToken을 Member에 저장한다`() {
        every { tokenValidatePort.validateRegistrationToken("apple-reg-token") } returns
            RegistrationTokenClaims(LoginProvider.APPLE, "apple-sub-1", "apple-refresh-1")
        every { memberQueryPort.findMemberIdByProvider(LoginProvider.APPLE, "apple-sub-1") } returns null
        every { tosQueryPort.findCurrentTerms() } returns listOf(requiredTerms)
        every { nicknameGenerator.generate() } returns "상큼한자두"
        every {
            memberRegistrar.register(LoginProvider.APPLE, "apple-sub-1", "상큼한자두", listOf(1L))
        } returns 99L
        every { tokenIssuePort.createAccessToken(99L) } returns "access-token"
        every { tokenIssuePort.createRefreshToken(99L, any()) } returns "refresh-token"

        service.signup("apple-reg-token", listOf(TermsAgreement(1L, true)))

        verify { memberAppleRefreshTokenSavePort.saveRefreshToken(99L, "apple-refresh-1") }
    }
}
