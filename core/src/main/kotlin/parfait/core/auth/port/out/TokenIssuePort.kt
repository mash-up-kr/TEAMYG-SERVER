package parfait.core.auth.port.out

import parfait.core.auth.domain.LoginProvider

interface TokenIssuePort {
    fun createAccessToken(memberId: Long): String

    fun createRefreshToken(
        memberId: Long,
        sessionId: String,
    ): String

    fun createRegistrationToken(
        provider: LoginProvider,
        providerUserId: String,
        appleRefreshToken: String? = null,
    ): String
}
