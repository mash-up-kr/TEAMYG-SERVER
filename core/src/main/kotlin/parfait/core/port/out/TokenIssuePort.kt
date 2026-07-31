package parfait.core.port.out

interface TokenIssuePort {
    fun createAccessToken(memberId: Long): String

    fun createRefreshToken(
        memberId: Long,
        sessionId: String,
    ): String
}
