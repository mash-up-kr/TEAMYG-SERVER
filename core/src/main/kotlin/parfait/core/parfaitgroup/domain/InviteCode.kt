package parfait.core.parfaitgroup.domain

@JvmInline
value class InviteCode private constructor(
    val value: String,
) {
    companion object {
        const val LENGTH = 6

        fun of(value: String): InviteCode {
            if (value.length != LENGTH || value.any { !it.isLetterOrDigit() || it.code > 127 }) {
                throw ParfaitGroupException(ParfaitGroupError.INVALID_INVITE_CODE)
            }
            return InviteCode(value.uppercase())
        }

        internal fun generated(value: String): InviteCode = InviteCode(value)
    }
}
