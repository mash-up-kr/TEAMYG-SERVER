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
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesUseCase
import parfait.http.parfaitimage.dto.UpdateParfaitImagesRequest
import parfait.http.parfaitimage.dto.UpdateParfaitImagesResponse

@Tag(name = "ParfaitImage")
@RestController
@RequestMapping("/api/v1/groups/{groupId}/parfaits/{parfaitId}/images")
class UpdateParfaitImagesController(
    private val updateParfaitImagesUseCase: UpdateParfaitImagesUseCase,
) {
    @Operation(summary = "토핑 위치/크기/각도 일괄 수정")
    @PatchMapping
    fun updateAll(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @PathVariable parfaitId: Long,
        @RequestBody request: UpdateParfaitImagesRequest,
    ): ApiResponse<UpdateParfaitImagesResponse> =
        ApiResponse.ok(
            UpdateParfaitImagesResponse.from(
                updateParfaitImagesUseCase.updateAll(
                    request.toCommand(authentication.memberId(), groupId, parfaitId),
                ),
            ),
        )

    private fun Authentication.memberId(): Long = name.toLong()
}
