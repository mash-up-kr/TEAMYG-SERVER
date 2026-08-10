@file:Suppress("ktlint:standard:package-name")

package parfait.core.auth.port.`in`

interface AppleLoginUseCase {
    fun login(
        identityToken: String,
        nonce: String,
        authorizationCode: String,
    ): AppleLoginResult
}

sealed interface AppleLoginResult {
    data class ExistingMember(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Long,
    ) : AppleLoginResult

    data class NewUser(
        val registrationToken: String,
    ) : AppleLoginResult
}
