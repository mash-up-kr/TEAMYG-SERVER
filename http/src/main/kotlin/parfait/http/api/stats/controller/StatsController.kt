package parfait.http.api.stats.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.stats.port.`in`.GetSignupStatsUseCase
import parfait.http.api.stats.dto.SignupStatsResponse
import java.time.LocalDate

/**
 * 이 경로는 SecurityConfig 화이트리스트(permitAll)라 JWT 인증을 거치지 않는다.
 * 이 컨트롤러의 모든 엔드포인트는 반드시 첫 줄에서 `statsTokenVerifier.verify(token)` 을 호출해야 한다.
 */
@Tag(name = "Stats")
@RestController
@RequestMapping("/admin/stats")
class StatsController(
    private val getSignupStatsUseCase: GetSignupStatsUseCase,
    private val statsTokenVerifier: StatsTokenVerifier,
) {
    @Operation(summary = "가입자 통계 조회 (X-Stats-Token 필요, 숫자만 응답)")
    @GetMapping
    fun getSignupStats(
        @RequestHeader("X-Stats-Token", required = false) token: String?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ApiResponse<SignupStatsResponse> {
        statsTokenVerifier.verify(token)
        val stats = getSignupStatsUseCase.getSignupStats(from.atStartOfDay(), to.atStartOfDay())
        return ApiResponse.ok(SignupStatsResponse.from(stats))
    }
}
