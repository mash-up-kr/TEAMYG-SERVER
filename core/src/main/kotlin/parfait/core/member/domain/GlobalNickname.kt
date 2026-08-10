package parfait.core.member.domain

import parfait.core.exception.BusinessException
import parfait.core.member.exception.MemberErrorCode

@JvmInline
value class GlobalNickname private constructor(
    val value: String,
) {
    companion object {
        private const val MAX_LENGTH = 15
        private val VALID_PATTERN = Regex("^[가-힣A-Za-z0-9]+(?: [가-힣A-Za-z0-9]+)*$")

        fun of(value: String): GlobalNickname {
            if (value.length !in 1..MAX_LENGTH || !VALID_PATTERN.matches(value)) {
                throw BusinessException(MemberErrorCode.INVALID_NICKNAME)
            }
            return GlobalNickname(value)
        }
    }
}
