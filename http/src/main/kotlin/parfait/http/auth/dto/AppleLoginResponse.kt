package parfait.http.auth.dto

data class AppleLoginResponse(
    val isNewUser: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val registrationToken: String? = null,
)
