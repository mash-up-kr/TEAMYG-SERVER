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
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageUseCase
import parfait.http.parfaitimage.dto.UpdateParfaitImageRequest
import parfait.http.parfaitimage.dto.UpdateParfaitImageResponse

@Tag(name = "ParfaitImage")
@RestController
@RequestMapping("/api/v1/groups/{groupId}/parfaits/{parfaitId}/images")
class UpdateParfaitImageController(
    private val updateParfaitImageUseCase: UpdateParfaitImageUseCase,
) {
    @Operation(summary = "토핑 위치/크기/각도 수정")
    @PatchMapping("/{parfaitImageId}")
    fun update(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @PathVariable parfaitId: Long,
        @PathVariable parfaitImageId: Long,
        @RequestBody request: UpdateParfaitImageRequest,
    ): ApiResponse<UpdateParfaitImageResponse> =
        ApiResponse.ok(
            UpdateParfaitImageResponse.from(
                updateParfaitImageUseCase.update(
                    request.toCommand(authentication.memberId(), groupId, parfaitId, parfaitImageId),
                ),
            ),
        )

    private fun Authentication.memberId(): Long = name.toLong()
}
