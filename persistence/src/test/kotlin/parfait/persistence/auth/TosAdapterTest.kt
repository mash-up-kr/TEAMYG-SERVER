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
import parfait.core.auth.port.out.CurrentTerms
import parfait.persistence.TestApplication
import parfait.persistence.entity.Tos
import parfait.persistence.entity.TosType
import parfait.persistence.repository.TosRepository
import java.time.LocalDateTime
import parfait.core.auth.domain.TosType as CoreTosType

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class TosAdapterTest {
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
    private lateinit var tosRepository: TosRepository

    @Test
    fun `타입별로 published_at이 가장 최신인 약관만 반환한다`() {
        val adapter = TosAdapter(tosRepository)
        // v2(2026-06-01)를 v1(2026-01-01)보다 먼저 저장 — id 순서와 published_at 순서를 일부러 어긋나게 해서,
        // id/삽입 순서가 아니라 published_at 기준으로 정렬하는지 검증한다.
        val latestTermsOfService =
            tosRepository.save(
                Tos(
                    type = TosType.TERMS_OF_SERVICE,
                    version = "2.0",
                    title = "이용약관 v2",
                    content = "내용 v2",
                    required = true,
                    publishedAt = LocalDateTime.of(2026, 6, 1, 0, 0),
                ),
            )
        tosRepository.save(
            Tos(
                type = TosType.TERMS_OF_SERVICE,
                version = "1.0",
                title = "이용약관 v1",
                content = "내용 v1",
                required = true,
                publishedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            ),
        )
        val privacyPolicy =
            tosRepository.save(
                Tos(
                    type = TosType.PRIVACY_POLICY,
                    version = "1.0",
                    title = "개인정보처리방침 v1",
                    content = "내용",
                    required = false,
                    publishedAt = LocalDateTime.of(2026, 3, 1, 0, 0),
                ),
            )

        val currentTerms = adapter.findCurrentTerms()

        currentTerms shouldContainExactlyInAnyOrder
            listOf(
                CurrentTerms(
                    id = latestTermsOfService.id!!,
                    type = CoreTosType.TERMS_OF_SERVICE,
                    title = "이용약관 v2",
                    url = "내용 v2",
                    required = true,
                ),
                CurrentTerms(
                    id = privacyPolicy.id!!,
                    type = CoreTosType.PRIVACY_POLICY,
                    title = "개인정보처리방침 v1",
                    url = "내용",
                    required = false,
                ),
            )
    }

    @Test
    fun `같은 타입에서 published_at이 동일하면 id가 더 큰 약관만 반환한다`() {
        val adapter = TosAdapter(tosRepository)
        val tiedAt = LocalDateTime.of(2026, 1, 1, 0, 0)
        tosRepository.save(
            Tos(
                type = TosType.TERMS_OF_SERVICE,
                version = "3.0",
                title = "이용약관 v3",
                content = "내용 v3",
                required = false,
                publishedAt = tiedAt,
            ),
        )
        val laterInsertedSameTimestamp =
            tosRepository.save(
                Tos(
                    type = TosType.TERMS_OF_SERVICE,
                    version = "4.0",
                    title = "이용약관 v4",
                    content = "내용 v4",
                    required = true,
                    publishedAt = tiedAt,
                ),
            )

        val currentTerms = adapter.findCurrentTerms()

        currentTerms shouldContainExactlyInAnyOrder
            listOf(
                CurrentTerms(
                    id = laterInsertedSameTimestamp.id!!,
                    type = CoreTosType.TERMS_OF_SERVICE,
                    title = "이용약관 v4",
                    url = "내용 v4",
                    required = true,
                ),
            )
    }
}
