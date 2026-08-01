package parfait.core.auth.port.out

interface TokenValidatePort {
    fun validateAccessToken(token: String): Long

    fun validateRefreshToken(token: String): RefreshTokenClaims
}

data class RefreshTokenClaims(
    val memberId: Long,
    val sessionId: String,
)
