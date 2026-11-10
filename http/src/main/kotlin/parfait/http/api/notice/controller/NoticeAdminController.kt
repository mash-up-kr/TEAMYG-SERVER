package parfait.http.api.notice.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.notice.port.`in`.RegisterNoticeUseCase
import parfait.core.notice.port.`in`.UpdateNoticeUseCase
import parfait.http.api.notice.dto.AdminNoticeResponse
import parfait.http.api.notice.dto.RegisterNoticeRequest
import parfait.http.api.notice.dto.UpdateNoticeRequest

/**
 * 이 경로는 SecurityConfig 화이트리스트(permitAll)라 JWT 인증을 거치지 않는다.
 * 이 컨트롤러의 모든 엔드포인트는 반드시 첫 줄에서 `noticeAdminTokenVerifier.verify(token)` 을 호출해야 한다.
 */
@Tag(name = "Notice Admin")
@RestController
@RequestMapping("/admin/notices")
class NoticeAdminController(
    private val registerNoticeUseCase: RegisterNoticeUseCase,
    private val updateNoticeUseCase: UpdateNoticeUseCase,
    private val noticeAdminTokenVerifier: NoticeAdminTokenVerifier,
) {
    @Operation(summary = "공지사항 등록 (X-Notice-Token 필요)")
    @PostMapping
    fun register(
        @RequestHeader("X-Notice-Token", required = false) token: String?,
        @Valid @RequestBody request: RegisterNoticeRequest,
    ): ApiResponse<AdminNoticeResponse> {
        noticeAdminTokenVerifier.verify(token)
        return ApiResponse.ok(AdminNoticeResponse.from(registerNoticeUseCase.register(request.toCommand())))
    }

    @Operation(summary = "공지사항 수정 (X-Notice-Token 필요)")
    @PatchMapping("/{id}")
    fun update(
        @RequestHeader("X-Notice-Token", required = false) token: String?,
        @PathVariable id: Long,
        @RequestBody request: UpdateNoticeRequest,
    ): ApiResponse<AdminNoticeResponse> {
        noticeAdminTokenVerifier.verify(token)
        return ApiResponse.ok(AdminNoticeResponse.from(updateNoticeUseCase.update(id, request.toCommand())))
    }
}
