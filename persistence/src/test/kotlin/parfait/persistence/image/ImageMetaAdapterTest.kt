package parfait.persistence.image

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.core.image.domain.ImageMeta
import parfait.core.image.domain.ImageStatus
import parfait.core.image.domain.ImageType
import parfait.persistence.TestApplication
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.repository.MemberRepository

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class ImageMetaAdapterTest {
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
    private lateinit var imageMetaAdapter: ImageMetaAdapter

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Test
    fun `저장하면 PENDING 상태와 imageType이 유지된 채 id가 채워진 도메인 객체를 반환한다`() {
        val member =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "image-adapter-test-user",
                    globalNickname = "이미지테스터",
                ),
            )

        val saved =
            imageMetaAdapter.save(
                ImageMeta.createPending(
                    url = "https://s3.example/nukki/user1/uuid.png",
                    uploadedByMemberId = requireNotNull(member.id),
                    imageType = ImageType.NUKKI,
                ),
            )

        saved.requireId() shouldBe saved.id
        saved.url shouldBe "https://s3.example/nukki/user1/uuid.png"
        saved.uploadedByMemberId shouldBe member.id
        saved.imageType shouldBe ImageType.NUKKI
        saved.status shouldBe ImageStatus.PENDING
        saved.referenceCount shouldBe 0
    }
}
