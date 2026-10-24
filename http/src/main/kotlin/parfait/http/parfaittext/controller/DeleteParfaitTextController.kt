package parfait.http.parfaittext.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfaittext.port.`in`.DeleteParfaitTextCommand
import parfait.core.parfaittext.port.`in`.DeleteParfaitTextUseCase

@Tag(name = "ParfaitText")
@RestController
@RequestMapping("/api/v1/groups/{groupId}/parfaits/{parfaitId}/texts")
class DeleteParfaitTextController(
    private val deleteParfaitTextUseCase: DeleteParfaitTextUseCase,
) {
    @Operation(summary = "텍스트 삭제")
    @DeleteMapping("/{parfaitTextId}")
    fun delete(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @PathVariable parfaitId: Long,
        @PathVariable parfaitTextId: Long,
    ): ApiResponse<Nothing?> {
        deleteParfaitTextUseCase.delete(
            DeleteParfaitTextCommand(
                memberId = authentication.name.toLong(),
                groupId = groupId,
                parfaitId = parfaitId,
                parfaitTextId = parfaitTextId,
            ),
        )
        return ApiResponse.ok(null)
    }
}
