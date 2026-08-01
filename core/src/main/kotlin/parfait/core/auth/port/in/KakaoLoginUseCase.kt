@file:Suppress("ktlint:standard:package-name")

package parfait.core.auth.port.`in`

interface KakaoLoginUseCase {
    fun login(
        idToken: String,
        nonce: String,
    ): KakaoLoginResult
}

sealed interface KakaoLoginResult {
    data class ExistingMember(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Long,
    ) : KakaoLoginResult

    data class NewUser(
        val registrationToken: String,
    ) : KakaoLoginResult
}
