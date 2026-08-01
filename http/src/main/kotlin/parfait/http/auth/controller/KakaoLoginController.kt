package parfait.http.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.auth.port.`in`.KakaoLoginResult
import parfait.core.auth.port.`in`.KakaoLoginUseCase
import parfait.http.auth.dto.KakaoLoginRequest
import parfait.http.auth.dto.KakaoLoginResponse

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth/kakao")
class KakaoLoginController(
    private val kakaoLoginUseCase: KakaoLoginUseCase,
) {
    @Operation(summary = "카카오 로그인")
    @PostMapping
    fun login(
        @Valid @RequestBody request: KakaoLoginRequest,
    ): ApiResponse<KakaoLoginResponse> =
        ApiResponse.ok(
            when (val result = kakaoLoginUseCase.login(request.idToken, request.nonce)) {
                is KakaoLoginResult.ExistingMember ->
                    KakaoLoginResponse(
                        isNewUser = false,
                        accessToken = result.accessToken,
                        refreshToken = result.refreshToken,
                        expiresIn = result.expiresIn,
                    )
                is KakaoLoginResult.NewUser ->
                    KakaoLoginResponse(
                        isNewUser = true,
                        registrationToken = result.registrationToken,
                    )
            },
        )
}
