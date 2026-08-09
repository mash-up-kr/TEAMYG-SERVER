package parfait.http.auth.dto

import jakarta.validation.constraints.NotBlank

data class AppleLoginRequest(
    @field:NotBlank val identityToken: String,
    @field:NotBlank val nonce: String,
    @field:NotBlank val authorizationCode: String,
)
