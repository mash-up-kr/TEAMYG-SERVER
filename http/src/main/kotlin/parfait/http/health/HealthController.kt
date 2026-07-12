package parfait.http.health

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse

@Tag(name = "Health")
@RestController
class HealthController {
    @Operation(summary = "서비스 상태 확인")
    @GetMapping("/health")
    fun health(): ApiResponse<HealthResponse> = ApiResponse.ok(HealthResponse(status = "UP"))
}

data class HealthResponse(
    val status: String,
)
