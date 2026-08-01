package parfait.http.auth.dto

data class ReissueResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
