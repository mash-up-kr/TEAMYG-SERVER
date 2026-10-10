package parfait.core.parfait.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CanvasEtagTest {
    private val etag = CanvasEtag.of(parfaitId = 100L, parfaitVersion = 42L, groupVersion = 3L, memberId = 7L)

    @Test
    fun `ETag 는 약한 ETag 형식으로 조립한다`() {
        etag shouldBe "W/\"p100-v42-g3-m7\""
    }

    @Test
    fun `같은 값을 그대로 돌려보내면 일치한다`() {
        CanvasEtag.matches(etag, etag) shouldBe true
    }

    @Test
    fun `W 접두어가 없어도 따옴표 안의 값이 같으면 일치한다`() {
        CanvasEtag.matches("\"p100-v42-g3-m7\"", etag) shouldBe true
    }

    @Test
    fun `쉼표로 여러 값이 오면 하나라도 같을 때 일치한다`() {
        CanvasEtag.matches("W/\"p1-v1-g1-m1\", $etag", etag) shouldBe true
    }

    @Test
    fun `별표는 일치로 처리한다`() {
        CanvasEtag.matches("*", etag) shouldBe true
    }

    @Test
    fun `값이 다르면 일치하지 않는다`() {
        CanvasEtag.matches("W/\"p100-v41-g3-m7\"", etag) shouldBe false
        CanvasEtag.matches("W/\"p100-v42-g3-m8\"", etag) shouldBe false
    }

    @Test
    fun `헤더가 없거나 비었거나 깨졌으면 일치하지 않는다`() {
        CanvasEtag.matches(null, etag) shouldBe false
        CanvasEtag.matches("", etag) shouldBe false
        CanvasEtag.matches("   ", etag) shouldBe false
        CanvasEtag.matches("garbage", etag) shouldBe false
    }
}
