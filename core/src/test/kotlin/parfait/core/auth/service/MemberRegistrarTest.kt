package parfait.core.auth.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.port.out.TosAgreementSavePort
import parfait.core.member.port.out.MemberCreatePort

class MemberRegistrarTest {
    private val memberCreatePort = mockk<MemberCreatePort>()
    private val tosAgreementSavePort = mockk<TosAgreementSavePort>(relaxed = true)
    private val registrar = MemberRegistrar(memberCreatePort, tosAgreementSavePort)

    @Test
    fun `회원을 생성하고 동의한 약관을 저장한 뒤 memberId를 반환한다`() {
        every {
            memberCreatePort.create(LoginProvider.KAKAO, "kakao-sub-1", "달콤한푸딩")
        } returns 42L

        val memberId = registrar.register(LoginProvider.KAKAO, "kakao-sub-1", "달콤한푸딩", listOf(1L, 2L))

        memberId shouldBe 42L
        verify { tosAgreementSavePort.saveAll(42L, listOf(1L, 2L)) }
    }
}
