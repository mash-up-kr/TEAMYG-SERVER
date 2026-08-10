package parfait.http.parfaitimage.controller

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
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageUseCase
import parfait.http.parfaitimage.dto.PlaceParfaitImageRequest
import parfait.http.parfaitimage.dto.PlaceParfaitImageResponse

@Tag(name = "ParfaitImage")
@RestController
@RequestMapping("/api/v1/groups/{groupId}/parfaits/{parfaitId}/images")
class PlaceParfaitImageController(
    private val placeParfaitImageUseCase: PlaceParfaitImageUseCase,
) {
    @Operation(summary = "토핑 배치 확정")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun place(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @PathVariable parfaitId: Long,
        @RequestBody request: PlaceParfaitImageRequest,
    ): ApiResponse<PlaceParfaitImageResponse> =
        ApiResponse.created(
            PlaceParfaitImageResponse.from(
                placeParfaitImageUseCase.place(request.toCommand(authentication.memberId(), groupId, parfaitId)),
            ),
        )

    private fun Authentication.memberId(): Long = name.toLong()
}
