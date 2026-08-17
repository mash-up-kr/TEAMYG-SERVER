package parfait.core.parfaitgroup.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ParfaitGroupValueObjectTest {
    @Test
    fun `그룹명은 한글 영문 숫자와 단일 중간 공백을 포함해 10자까지 허용한다`() {
        GroupName.of("우리 Group1").value shouldBe "우리 Group1"
    }

    @Test
    fun `그룹명의 잘못된 길이 문자 공백은 거부한다`() {
        listOf("", "12345678901", " 앞공백", "뒤공백 ", "연속  공백", "특수!문자", "이모지😀")
            .forEach { value ->
                assertGroupError(ParfaitGroupError.INVALID_GROUP_NAME) { GroupName.of(value) }
            }
    }

    @Test
    fun `그룹 닉네임은 한글 영문 숫자와 단일 중간 공백을 포함해 15자까지 허용한다`() {
        GroupNickname.of("파르페 Member12").value shouldBe "파르페 Member12"
    }

    @Test
    fun `그룹 닉네임의 잘못된 길이 문자 공백은 거부한다`() {
        listOf("", "1234567890123456", " 앞공백", "뒤공백 ", "연속  공백", "특수!문자", "이모지😀")
            .forEach { value ->
                assertGroupError(ParfaitGroupError.INVALID_GROUP_NICKNAME) { GroupNickname.of(value) }
            }
    }

    @Test
    fun `그룹 닉네임은 자음 모음 단독 입력도 허용한다`() {
        GroupNickname.of("ㅋㅋㅋ").value shouldBe "ㅋㅋㅋ"
        GroupNickname.of("ㅏㅑㅓ").value shouldBe "ㅏㅑㅓ"
        GroupNickname.of("자모 ㅠㅠ").value shouldBe "자모 ㅠㅠ"
    }

    @Test
    fun `그룹 인원은 1명부터 12명까지 허용한다`() {
        GroupMemberLimit.of(1).value shouldBe 1
        GroupMemberLimit.of(12).value shouldBe 12

        listOf(0, 13).forEach { value ->
            assertGroupError(ParfaitGroupError.INVALID_GROUP_MEMBER_LIMIT) {
                GroupMemberLimit.of(value)
            }
        }
    }

    @Test
    fun `초대코드는 영문 숫자 6자이며 소문자는 대문자로 정규화한다`() {
        InviteCode.of("abcd12").value shouldBe "ABCD12"

        listOf("", "ABC12", "ABCDEFG", "ABC-12", "가나다123").forEach { value ->
            assertGroupError(ParfaitGroupError.INVALID_INVITE_CODE) { InviteCode.of(value) }
        }
    }

    private fun assertGroupError(
        expected: ParfaitGroupError,
        block: () -> Unit,
    ) {
        assertFailsWith<ParfaitGroupException>(block = block).error shouldBe expected
    }
}
