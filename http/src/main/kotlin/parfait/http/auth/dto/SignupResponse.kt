package parfait.http.auth.dto

data class SignupResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
