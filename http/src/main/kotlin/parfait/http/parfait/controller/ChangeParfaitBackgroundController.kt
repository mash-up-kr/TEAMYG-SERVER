package parfait.http.parfait.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfait.port.`in`.ChangeParfaitBackgroundUseCase
import parfait.http.parfait.dto.ChangeParfaitBackgroundRequest
import parfait.http.parfait.dto.ChangeParfaitBackgroundResponse

@Tag(name = "Parfait")
@RestController
@RequestMapping("/api/v1/groups/{groupId}/parfaits/{parfaitId}/background")
class ChangeParfaitBackgroundController(
    private val changeParfaitBackgroundUseCase: ChangeParfaitBackgroundUseCase,
) {
    @Operation(summary = "캔버스 배경 변경")
    @PatchMapping
    fun change(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @PathVariable parfaitId: Long,
        @RequestBody request: ChangeParfaitBackgroundRequest,
    ): ApiResponse<ChangeParfaitBackgroundResponse> =
        ApiResponse.ok(
            ChangeParfaitBackgroundResponse.from(
                changeParfaitBackgroundUseCase.change(request.toCommand(authentication.memberId(), groupId, parfaitId)),
            ),
        )

    private fun Authentication.memberId(): Long = name.toLong()
}
