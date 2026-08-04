package parfait.http.image.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.image.port.`in`.ConfirmImageUploadCommand
import parfait.core.image.port.`in`.ConfirmImageUploadUseCase
import parfait.http.image.dto.ConfirmImageUploadResponse

@Tag(name = "Image")
@RestController
@RequestMapping("/api/v1/images")
class ConfirmImageUploadController(
    private val confirmImageUploadUseCase: ConfirmImageUploadUseCase,
) {
    @Operation(summary = "이미지 업로드 확인")
    @PostMapping("/{imageId}/confirm")
    fun confirm(
        @PathVariable imageId: Long,
    ): ApiResponse<ConfirmImageUploadResponse> =
        ApiResponse.ok(
            ConfirmImageUploadResponse.from(
                confirmImageUploadUseCase.confirm(ConfirmImageUploadCommand(imageId)),
            ),
        )
}
