package parfait.core.auth.port.out

interface TokenDeletePort {
    fun delete(
        memberId: Long,
        sessionId: String,
    )

    fun deleteAllByMemberId(memberId: Long)
}
