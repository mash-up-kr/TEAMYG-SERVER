package parfait.http.image.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.image.port.`in`.IssueImageUploadUrlUseCase
import parfait.http.image.dto.IssueImageUploadUrlRequest
import parfait.http.image.dto.IssueImageUploadUrlResponse

@Tag(name = "Image")
@RestController
@RequestMapping("/api/v1/images")
class IssueImageUploadUrlController(
    private val issueImageUploadUrlUseCase: IssueImageUploadUrlUseCase,
) {
    @Operation(summary = "이미지 업로드 URL 발급")
    @PostMapping
    fun issue(
        authentication: Authentication,
        @Valid @RequestBody request: IssueImageUploadUrlRequest,
    ): ApiResponse<IssueImageUploadUrlResponse> =
        ApiResponse.ok(
            IssueImageUploadUrlResponse.from(
                issueImageUploadUrlUseCase.issue(request.toCommand(authentication.memberId())),
            ),
        )

    private fun Authentication.memberId(): Long = name.toLong()
}
