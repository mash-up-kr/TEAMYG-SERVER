package parfait.http.parfait.controller

import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfait.port.`in`.GetParfaitYearsUseCase
import parfait.core.parfait.port.`in`.GetTodayParfaitCommand
import parfait.core.parfait.port.`in`.GetTodayParfaitUseCase
import parfait.http.parfait.dto.GetTodayParfaitResponse
import parfait.http.parfait.dto.ParfaitYearsResponse

@RestController
@RequestMapping("/api/v1/groups/{groupId}/parfaits")
class ParfaitController(
    private val getParfaitYearsUseCase: GetParfaitYearsUseCase,
    private val getTodayParfaitUseCase: GetTodayParfaitUseCase,
) {
    @GetMapping("/year")
    fun getYears(
        authentication: Authentication,
        @PathVariable groupId: Long,
    ): ApiResponse<ParfaitYearsResponse> =
        ApiResponse.ok(
            ParfaitYearsResponse(getParfaitYearsUseCase.getYears(authentication.memberId(), groupId)),
        )

    @GetMapping("/today")
    fun getToday(
        authentication: Authentication,
        @PathVariable groupId: Long,
    ): ApiResponse<GetTodayParfaitResponse> =
        ApiResponse.ok(
            GetTodayParfaitResponse.from(
                getTodayParfaitUseCase.get(
                    GetTodayParfaitCommand(memberId = authentication.memberId(), groupId = groupId),
                ),
            ),
        )

    private fun Authentication.memberId(): Long = name.toLong()
}
