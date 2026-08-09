package parfait.core.member.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.member.exception.MemberErrorCode
import kotlin.test.assertFailsWith

class GlobalNicknameTest {
    @Test
    fun `전역 닉네임은 한글 영문 숫자와 단일 중간 공백을 포함해 15자까지 허용한다`() {
        GlobalNickname.of("부지런한 수달").value shouldBe "부지런한 수달"
        GlobalNickname.of("a").value shouldBe "a"
    }

    @Test
    fun `허용되지 않는 길이 문자 공백은 INVALID_NICKNAME 예외를 던진다`() {
        listOf("", "1234567890123456", " 앞공백", "뒤공백 ", "연속  공백", "특수!문자", "이모지😀")
            .forEach { value ->
                val exception = assertFailsWith<BusinessException> { GlobalNickname.of(value) }
                exception.errorCode shouldBe MemberErrorCode.INVALID_NICKNAME
            }
    }
}
