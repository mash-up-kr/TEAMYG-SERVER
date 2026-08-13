package parfait.core.parfaitgroup.domain

@JvmInline
value class GroupNickname private constructor(
    val value: String,
) {
    companion object {
        private const val MAX_LENGTH = 15
        private const val UNKNOWN_NICKNAME = "(알수없음)"
        private val VALID_PATTERN = Regex("^[가-힣A-Za-z0-9]+(?: [가-힣A-Za-z0-9]+)*$")

        fun of(value: String): GroupNickname {
            if (value == UNKNOWN_NICKNAME) {
                return GroupNickname(value)
            }
            if (value.length !in 1..MAX_LENGTH || !VALID_PATTERN.matches(value)) {
                throw ParfaitGroupException(ParfaitGroupError.INVALID_GROUP_NICKNAME)
            }
            return GroupNickname(value)
        }

        fun unknown(): GroupNickname = GroupNickname(UNKNOWN_NICKNAME)
    }
}
