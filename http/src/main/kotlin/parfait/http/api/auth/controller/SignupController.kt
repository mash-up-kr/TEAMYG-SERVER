package parfait.http.api.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.auth.port.`in`.SignupUseCase
import parfait.core.auth.port.`in`.TermsAgreement
import parfait.http.api.auth.dto.SignupRequest
import parfait.http.api.auth.dto.SignupResponse

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth/signup")
class SignupController(
    private val signupUseCase: SignupUseCase,
) {
    @Operation(summary = "회원가입 완료")
    @PostMapping
    fun signup(
        @Valid @RequestBody request: SignupRequest,
    ): ResponseEntity<ApiResponse<SignupResponse>> {
        val result =
            signupUseCase.signup(
                registrationToken = request.registrationToken,
                agreements = request.agreements.map { TermsAgreement(it.termsId, it.agreed) },
            )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponse.created(
                    SignupResponse(
                        accessToken = result.accessToken,
                        refreshToken = result.refreshToken,
                        expiresIn = result.expiresIn,
                    ),
                ),
            )
    }
}
