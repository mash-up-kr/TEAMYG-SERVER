package parfait.core.member.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import parfait.core.member.domain.RandomNicknameGenerator.Companion.INGREDIENTS
import parfait.core.member.domain.RandomNicknameGenerator.Companion.MODIFIERS

class RandomNicknameGeneratorTest {
    private val generator = RandomNicknameGenerator()

    @Test
    fun `형용사 동사 풀은 100개, 재료 풀은 30개다`() {
        MODIFIERS.size shouldBe 100
        INGREDIENTS.size shouldBe 30
    }

    @Test
    fun `생성된 닉네임은 형용사 동사와 재료 풀 조합 중 하나와 정확히 일치한다`() {
        repeat(200) {
            val nickname = generator.generate()

            val isValidCombination =
                MODIFIERS.any { modifier -> INGREDIENTS.any { ingredient -> "$modifier$ingredient" == nickname } }

            isValidCombination shouldBe true
        }
    }

    @Test
    fun `생성된 닉네임에는 공백이 없다`() {
        repeat(50) {
            generator.generate() shouldNotContain " "
        }
    }
}
