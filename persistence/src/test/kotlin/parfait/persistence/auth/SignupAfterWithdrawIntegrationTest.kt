package parfait.persistence.auth

import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.domain.TosType
import parfait.core.auth.port.`in`.TermsAgreement
import parfait.core.auth.port.out.CurrentTerms
import parfait.core.auth.port.out.RegistrationTokenClaims
import parfait.core.auth.port.out.TokenIssuePort
import parfait.core.auth.port.out.TokenSavePort
import parfait.core.auth.port.out.TokenValidatePort
import parfait.core.auth.port.out.TosQueryPort
import parfait.core.auth.service.MemberRegistrar
import parfait.core.auth.service.SignupService
import parfait.core.member.domain.RandomNicknameGenerator
import parfait.persistence.TestApplication
import parfait.persistence.entity.Tos
import parfait.persistence.member.MemberAdapter
import parfait.persistence.repository.MemberRepository
import parfait.persistence.repository.TosAgreementRepository
import parfait.persistence.repository.TosRepository
import parfait.persistence.entity.TosType as PersistenceTosType

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class SignupAfterWithdrawIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.4")

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
        }
    }

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var tosAgreementRepository: TosAgreementRepository

    @Autowired
    private lateinit var tosRepository: TosRepository

    @Test
    fun `탈퇴 후 같은 provider로 재가입해도 500이 나지 않는다`() {
        val memberAdapter = MemberAdapter(memberRepository)
        val tosAgreementAdapter = TosAgreementAdapter(tosAgreementRepository)
        val memberRegistrar = MemberRegistrar(memberAdapter, tosAgreementAdapter)

        val tos =
            tosRepository.save(
                Tos(
                    type = PersistenceTosType.TERMS_OF_SERVICE,
                    version = "1.0",
                    title = "이용약관",
                    content = "내용",
                    required = true,
                ),
            )

        val tokenValidatePort = mockk<TokenValidatePort>()
        every { tokenValidatePort.validateRegistrationToken("token-1") } returns
            RegistrationTokenClaims(LoginProvider.APPLE, "apple-sub-1")
        every { tokenValidatePort.validateRegistrationToken("token-2") } returns
            RegistrationTokenClaims(LoginProvider.APPLE, "apple-sub-1")

        val tosQueryPort = mockk<TosQueryPort>()
        every { tosQueryPort.findCurrentTerms() } returns
            listOf(CurrentTerms(tos.id!!, TosType.TERMS_OF_SERVICE, "이용약관", "https://x", true))

        val tokenIssuePort = mockk<TokenIssuePort>()
        every { tokenIssuePort.createAccessToken(any(), any()) } returns "access"
        every { tokenIssuePort.createRefreshToken(any(), any()) } returns "refresh"

        val tokenSavePort = mockk<TokenSavePort>(relaxed = true)

        val signupService =
            SignupService(
                tokenValidatePort = tokenValidatePort,
                memberQueryPort = memberAdapter,
                tosQueryPort = tosQueryPort,
                memberRegistrar = memberRegistrar,
                tokenIssuePort = tokenIssuePort,
                tokenSavePort = tokenSavePort,
                nicknameGenerator = RandomNicknameGenerator(),
                accessTokenExpiresInSeconds = 3600,
                refreshTokenTtlSeconds = 1_209_600,
            )

        val firstResult = signupService.signup("token-1", listOf(TermsAgreement(tos.id!!, true)))
        firstResult shouldNotBe null

        val firstMemberId = memberAdapter.findMemberIdByProvider(LoginProvider.APPLE, "apple-sub-1")!!
        memberAdapter.deleteById(firstMemberId)

        val secondResult = signupService.signup("token-2", listOf(TermsAgreement(tos.id!!, true)))
        secondResult shouldNotBe null
    }
}
