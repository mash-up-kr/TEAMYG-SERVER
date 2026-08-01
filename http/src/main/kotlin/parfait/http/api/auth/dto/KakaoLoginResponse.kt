package parfait.http.api.auth.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KakaoLoginResponse(
    val isNewUser: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val registrationToken: String? = null,
)
