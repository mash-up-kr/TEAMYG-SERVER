package parfait.core.appversion.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class SemVerTest {
    @Test
    fun `올바른 형식의 버전 문자열을 파싱한다`() {
        val semVer = SemVer.parse("1.4.0")
        semVer.major shouldBe 1
        semVer.minor shouldBe 4
        semVer.patch shouldBe 0
    }

    @Test
    fun `toString 은 major-minor-patch 형식으로 반환한다`() {
        SemVer.parse("1.4.0").toString() shouldBe "1.4.0"
    }

    @Test
    fun `1-10-0 은 1-9-0 보다 크다 (문자열 정렬과 다름)`() {
        (SemVer.parse("1.10.0") > SemVer.parse("1.9.0")) shouldBe true
    }

    @Test
    fun `같은 버전은 동등하다`() {
        (SemVer.parse("1.4.0") == SemVer.parse("1.4.0")) shouldBe true
    }

    @Test
    fun `patch 가 더 크면 더 크다`() {
        (SemVer.parse("1.4.1") > SemVer.parse("1.4.0")) shouldBe true
    }

    @Test
    fun `알파 문자가 포함된 버전은 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> { SemVer.parse("1.a.0") }
    }

    @Test
    fun `컴포넌트가 2개뿐인 버전은 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> { SemVer.parse("1.4") }
    }

    @Test
    fun `빈 문자열은 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> { SemVer.parse("") }
    }
}
