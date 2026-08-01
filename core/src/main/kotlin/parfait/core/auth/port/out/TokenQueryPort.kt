package parfait.core.auth.port.out

interface TokenQueryPort {
    fun findRefreshToken(
        memberId: Long,
        sessionId: String,
    ): String?
}
