package parfait.http.api.auth.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class SignupRequest(
    @field:NotBlank val registrationToken: String,
    @field:Valid val agreements: List<TermsAgreementRequest>,
)

data class TermsAgreementRequest(
    val termsId: Long,
    val agreed: Boolean,
)
