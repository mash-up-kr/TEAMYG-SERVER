package parfait.http.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.auth.port.`in`.ReissueUseCase
import parfait.http.auth.dto.ReissueRequest
import parfait.http.auth.dto.ReissueResponse

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth/reissue")
class ReissueController(
    private val reissueUseCase: ReissueUseCase,
) {
    @Operation(summary = "토큰 재발급")
    @PostMapping
    fun reissue(
        @Valid @RequestBody request: ReissueRequest,
    ): ApiResponse<ReissueResponse> {
        val result = reissueUseCase.reissue(request.refreshToken)
        return ApiResponse.ok(
            ReissueResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                expiresIn = result.expiresIn,
            ),
        )
    }
}
