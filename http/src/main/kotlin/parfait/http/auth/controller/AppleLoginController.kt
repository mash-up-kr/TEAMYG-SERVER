package parfait.http.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.auth.port.`in`.AppleLoginResult
import parfait.core.auth.port.`in`.AppleLoginUseCase
import parfait.http.auth.dto.AppleLoginRequest
import parfait.http.auth.dto.AppleLoginResponse

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth/apple")
class AppleLoginController(
    private val appleLoginUseCase: AppleLoginUseCase,
) {
    @Operation(summary = "애플 로그인")
    @PostMapping
    fun login(
        @Valid @RequestBody request: AppleLoginRequest,
    ): ApiResponse<AppleLoginResponse> =
        ApiResponse.ok(
            when (
                val result =
                    appleLoginUseCase.login(request.identityToken, request.nonce)
            ) {
                is AppleLoginResult.ExistingMember ->
                    AppleLoginResponse(
                        isNewUser = false,
                        accessToken = result.accessToken,
                        refreshToken = result.refreshToken,
                        expiresIn = result.expiresIn,
                    )
                is AppleLoginResult.NewUser ->
                    AppleLoginResponse(
                        isNewUser = true,
                        registrationToken = result.registrationToken,
                    )
            },
        )
}
