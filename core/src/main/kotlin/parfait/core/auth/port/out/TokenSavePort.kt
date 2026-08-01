package parfait.core.auth.port.out

interface TokenSavePort {
    fun save(
        memberId: Long,
        sessionId: String,
        refreshToken: String,
        ttlSeconds: Long,
    )
}
