package parfait.http.parfait.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfait.port.`in`.RotateParfaitCanvasesUseCase
import parfait.http.parfait.dto.RotateParfaitCanvasesResponse

// TODO: 테스트 전용 엔드포인트 — 실제 프로덕션 오픈 전 이 컨트롤러와
// SecurityConfig.WHITELIST_PATHS의 "/api/v1/test/parfait-canvas/rotate" 항목을 함께 제거할 것
@Tag(name = "Test")
@RestController
@RequestMapping("/api/v1/test/parfait-canvas")
class ParfaitCanvasRotationTestController(
    private val rotateParfaitCanvasesUseCase: RotateParfaitCanvasesUseCase,
) {
    @Operation(
        summary = "캔버스 강제 회전 (테스트 전용)",
        description = "⚠️ 인증 없이 호출 가능 · 전체 그룹의 ACTIVE 캔버스를 즉시 마감하고 새로 생성합니다. 테스트 전용",
    )
    @PostMapping("/rotate")
    fun rotate(): ApiResponse<RotateParfaitCanvasesResponse> {
        val result = rotateParfaitCanvasesUseCase.rotateAll()
        return ApiResponse.ok(
            RotateParfaitCanvasesResponse(
                closedCount = result.closedCount,
                emptyCount = result.emptyCount,
                createdCount = result.createdCount,
                failedCount = result.failedCount,
            ),
        )
    }
}
