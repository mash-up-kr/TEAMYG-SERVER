package parfait.persistence.auth

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.persistence.TestApplication
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.entity.Tos
import parfait.persistence.entity.TosType
import parfait.persistence.repository.MemberRepository
import parfait.persistence.repository.TosAgreementRepository
import parfait.persistence.repository.TosRepository

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class TosAgreementAdapterTest {
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
    private lateinit var tosAgreementRepository: TosAgreementRepository

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var tosRepository: TosRepository

    @Test
    fun `전달한 tosId 각각에 대해 회원의 약관 동의 이력을 저장한다`() {
        val adapter = TosAgreementAdapter(tosAgreementRepository)
        val member =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "agreement-test-user",
                    globalNickname = "닉네임",
                ),
            )
        val termsOfService =
            tosRepository.save(
                Tos(
                    type = TosType.TERMS_OF_SERVICE,
                    version = "1.0",
                    title = "이용약관",
                    content = "내용",
                    required = true,
                ),
            )
        val privacyPolicy =
            tosRepository.save(
                Tos(
                    type = TosType.PRIVACY_POLICY,
                    version = "1.0",
                    title = "개인정보처리방침",
                    content = "내용",
                    required = false,
                ),
            )

        adapter.saveAll(member.id!!, listOf(termsOfService.id!!, privacyPolicy.id!!))

        val saved = tosAgreementRepository.findAll().filter { it.memberId == member.id }
        saved.map { it.tosId } shouldContainExactlyInAnyOrder listOf(termsOfService.id!!, privacyPolicy.id!!)
    }
}
