package parfait.http.parfaitimage.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderUseCase
import parfait.http.parfaitimage.dto.UpdateParfaitImageBorderRequest
import parfait.http.parfaitimage.dto.UpdateParfaitImageBorderResponse

@Tag(name = "ParfaitImage")
@RestController
@RequestMapping("/api/v1/groups/{groupId}/parfaits/{parfaitId}/images")
class UpdateParfaitImageBorderController(
    private val updateParfaitImageBorderUseCase: UpdateParfaitImageBorderUseCase,
) {
    @Operation(summary = "토핑 테두리 두께/색깔 수정")
    @PatchMapping("/{parfaitImageId}/border")
    fun update(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @PathVariable parfaitId: Long,
        @PathVariable parfaitImageId: Long,
        @RequestBody request: UpdateParfaitImageBorderRequest,
    ): ApiResponse<UpdateParfaitImageBorderResponse> =
        ApiResponse.ok(
            UpdateParfaitImageBorderResponse.from(
                updateParfaitImageBorderUseCase.update(
                    request.toCommand(authentication.memberId(), groupId, parfaitId, parfaitImageId),
                ),
            ),
        )

    private fun Authentication.memberId(): Long = name.toLong()
}
