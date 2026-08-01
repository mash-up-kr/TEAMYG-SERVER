package parfait.core.parfaitgroup.domain

@JvmInline
value class GroupName private constructor(
    val value: String,
) {
    companion object {
        private const val MAX_LENGTH = 10
        private val VALID_PATTERN = Regex("^[가-힣A-Za-z0-9]+(?: [가-힣A-Za-z0-9]+)*$")

        fun of(value: String): GroupName {
            if (value.length !in 1..MAX_LENGTH || !VALID_PATTERN.matches(value)) {
                throw ParfaitGroupException(ParfaitGroupError.INVALID_GROUP_NAME)
            }
            return GroupName(value)
        }
    }
}
