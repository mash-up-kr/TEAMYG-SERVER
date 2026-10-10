package parfait.http.parfait.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedCommand
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedResult
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedUseCase
import parfait.http.parfait.dto.GetTodayParfaitResponse

/**
 * v1 today 는 그대로 두고, ETag/304 를 지원하는 v2 를 따로 둔다. 응답 본문은 v1 과 같은 구조(`GetTodayParfaitResponse`).
 * 304 는 본문과 공통 응답 래퍼 없이 헤더(ETag, Cache-Control)만 내려간다.
 */
@RestController
@RequestMapping("/api/v2/groups/{groupId}/parfaits")
class ParfaitV2Controller(
    private val getTodayParfaitIfChangedUseCase: GetTodayParfaitIfChangedUseCase,
) {
    @GetMapping("/today")
    fun getToday(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @RequestHeader(HttpHeaders.IF_NONE_MATCH, required = false) ifNoneMatch: String?,
    ): ResponseEntity<ApiResponse<GetTodayParfaitResponse>> {
        val result =
            getTodayParfaitIfChangedUseCase.get(
                GetTodayParfaitIfChangedCommand(
                    memberId = authentication.name.toLong(),
                    groupId = groupId,
                    ifNoneMatch = ifNoneMatch,
                ),
            )
        return when (result) {
            is GetTodayParfaitIfChangedResult.NotModified ->
                ResponseEntity
                    .status(HttpStatus.NOT_MODIFIED)
                    .header(HttpHeaders.ETAG, result.etag)
                    .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                    .build()

            is GetTodayParfaitIfChangedResult.Modified ->
                ResponseEntity
                    .ok()
                    .header(HttpHeaders.ETAG, result.etag)
                    .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                    .body(ApiResponse.ok(GetTodayParfaitResponse.from(result.body)))
        }
    }

    private companion object {
        const val CACHE_CONTROL = "private, no-cache"
    }
}
