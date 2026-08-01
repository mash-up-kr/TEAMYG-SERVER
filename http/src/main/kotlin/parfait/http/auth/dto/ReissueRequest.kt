package parfait.http.auth.dto

import jakarta.validation.constraints.NotBlank

data class ReissueRequest(
    @field:NotBlank val refreshToken: String,
)
