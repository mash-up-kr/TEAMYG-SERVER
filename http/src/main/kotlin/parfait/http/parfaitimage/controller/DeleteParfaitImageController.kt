package parfait.http.parfaitimage.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfaitimage.port.`in`.DeleteParfaitImageCommand
import parfait.core.parfaitimage.port.`in`.DeleteParfaitImageUseCase

@Tag(name = "ParfaitImage")
@RestController
@RequestMapping("/api/v1/groups/{groupId}/parfaits/{parfaitId}/images")
class DeleteParfaitImageController(
    private val deleteParfaitImageUseCase: DeleteParfaitImageUseCase,
) {
    @Operation(summary = "토핑 삭제")
    @DeleteMapping("/{parfaitImageId}")
    fun delete(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @PathVariable parfaitId: Long,
        @PathVariable parfaitImageId: Long,
    ): ApiResponse<Nothing?> {
        deleteParfaitImageUseCase.delete(
            DeleteParfaitImageCommand(
                memberId = authentication.memberId(),
                groupId = groupId,
                parfaitId = parfaitId,
                parfaitImageId = parfaitImageId,
            ),
        )
        return ApiResponse.ok(null)
    }

    private fun Authentication.memberId(): Long = name.toLong()
}
