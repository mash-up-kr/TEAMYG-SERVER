package parfait.http.api.notice.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.port.`in`.GetNoticesUseCase
import parfait.http.api.notice.dto.NoticeResponse

@Tag(name = "Notice")
@RestController
@RequestMapping("/api/v1/notices")
class NoticeController(
    private val getNoticesUseCase: GetNoticesUseCase,
) {
    @Operation(summary = "공지사항 목록 조회 (platform: IOS 또는 ANDROID, 해당 플랫폼 + 공통 공지를 최신순으로)")
    @GetMapping
    fun getNotices(
        @RequestParam platform: NoticePlatform,
    ): ApiResponse<List<NoticeResponse>> =
        ApiResponse.ok(getNoticesUseCase.getNotices(platform).map(NoticeResponse::from))
}
