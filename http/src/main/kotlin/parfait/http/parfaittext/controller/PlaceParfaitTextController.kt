package parfait.http.parfaittext.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfaittext.port.`in`.PlaceParfaitTextUseCase
import parfait.http.parfait.dto.TodayParfaitTextResponse
import parfait.http.parfaittext.dto.PlaceParfaitTextRequest

@Tag(name = "ParfaitText")
@RestController
@RequestMapping("/api/v1/groups/{groupId}/parfaits/{parfaitId}/texts")
class PlaceParfaitTextController(
    private val placeParfaitTextUseCase: PlaceParfaitTextUseCase,
) {
    @Operation(summary = "텍스트 배치 확정")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun place(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @PathVariable parfaitId: Long,
        @RequestBody request: PlaceParfaitTextRequest,
    ): ApiResponse<TodayParfaitTextResponse> =
        ApiResponse.created(
            TodayParfaitTextResponse.from(
                placeParfaitTextUseCase.place(request.toCommand(authentication.name.toLong(), groupId, parfaitId)),
            ),
        )
}
