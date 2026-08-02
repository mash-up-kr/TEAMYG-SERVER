package parfait.core.auth.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.auth.domain.TosType
import parfait.core.auth.port.out.CurrentTerms
import parfait.core.auth.port.out.TosQueryPort

class PolicyQueryServiceTest {
    private val tosQueryPort = mockk<TosQueryPort>()
    private val service = PolicyQueryService(tosQueryPort)

    private val termsOfService =
        CurrentTerms(
            id = 1L,
            type = TosType.TERMS_OF_SERVICE,
            title = "서비스 이용약관",
            url = "https://parfait.app/policies/terms",
            required = true,
        )
    private val privacyPolicy =
        CurrentTerms(
            id = 2L,
            type = TosType.PRIVACY_POLICY,
            title = "개인정보 처리방침",
            url = "https://parfait.app/policies/privacy",
            required = true,
        )

    @Test
    fun `포트가 어떤 순서로 반환하든 TERMS_OF_SERVICE, PRIVACY_POLICY 순서로 반환한다`() {
        every { tosQueryPort.findCurrentTerms() } returns listOf(privacyPolicy, termsOfService)

        val result = service.getPolicies()

        result shouldBe listOf(termsOfService, privacyPolicy)
    }

    @Test
    fun `PRIVACY_POLICY가 없으면 TERMS_OF_SERVICE만 반환한다`() {
        every { tosQueryPort.findCurrentTerms() } returns listOf(termsOfService)

        val result = service.getPolicies()

        result shouldBe listOf(termsOfService)
    }

    @Test
    fun `현재 약관이 하나도 없으면 빈 리스트를 반환한다`() {
        every { tosQueryPort.findCurrentTerms() } returns emptyList()

        val result = service.getPolicies()

        result shouldBe emptyList()
    }
}
