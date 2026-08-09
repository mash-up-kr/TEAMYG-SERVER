package parfait.core.auth.port.out

import parfait.core.auth.domain.LoginProvider

interface TokenValidatePort {
    fun validateAccessToken(token: String): Long

    fun validateRefreshToken(token: String): RefreshTokenClaims

    fun validateRegistrationToken(token: String): RegistrationTokenClaims
}

data class RefreshTokenClaims(
    val memberId: Long,
    val sessionId: String,
)

data class RegistrationTokenClaims(
    val provider: LoginProvider,
    val providerUserId: String,
    val appleRefreshToken: String? = null,
)
