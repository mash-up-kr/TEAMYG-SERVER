package parfait.http.parfaittext.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextUseCase
import parfait.http.parfaittext.dto.UpdateParfaitTextRequest
import parfait.http.parfaittext.dto.UpdateParfaitTextResponse

@Tag(name = "ParfaitText")
@RestController
@RequestMapping("/api/v1/groups/{groupId}/parfaits/{parfaitId}/texts")
class UpdateParfaitTextController(
    private val updateParfaitTextUseCase: UpdateParfaitTextUseCase,
) {
    @Operation(summary = "텍스트 위치/크기/각도/색상/채움/외곽선 수정")
    @PatchMapping("/{parfaitTextId}")
    fun update(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @PathVariable parfaitId: Long,
        @PathVariable parfaitTextId: Long,
        @RequestBody request: UpdateParfaitTextRequest,
    ): ApiResponse<UpdateParfaitTextResponse> =
        ApiResponse.ok(
            UpdateParfaitTextResponse.from(
                updateParfaitTextUseCase.update(
                    request.toCommand(authentication.name.toLong(), groupId, parfaitId, parfaitTextId),
                ),
            ),
        )
}
