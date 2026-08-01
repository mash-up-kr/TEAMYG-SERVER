package parfait.core.parfaitgroup.domain

@JvmInline
value class GroupMemberLimit private constructor(
    val value: Int,
) {
    companion object {
        const val MIN = 1
        const val MAX = 12

        fun of(value: Int): GroupMemberLimit {
            if (value !in MIN..MAX) {
                throw ParfaitGroupException(ParfaitGroupError.INVALID_GROUP_MEMBER_LIMIT)
            }
            return GroupMemberLimit(value)
        }
    }
}
