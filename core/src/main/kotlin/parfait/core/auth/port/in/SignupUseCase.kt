@file:Suppress("ktlint:standard:package-name")

package parfait.core.auth.port.`in`

interface SignupUseCase {
    fun signup(
        registrationToken: String,
        agreements: List<TermsAgreement>,
    ): SignupResult
}

data class TermsAgreement(
    val termsId: Long,
    val agreed: Boolean,
)

data class SignupResult(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
