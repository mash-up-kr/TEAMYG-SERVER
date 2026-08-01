package parfait.http.api.auth.dto

import jakarta.validation.constraints.NotBlank

data class KakaoLoginRequest(
    @field:NotBlank val idToken: String,
    @field:NotBlank val nonce: String,
)
