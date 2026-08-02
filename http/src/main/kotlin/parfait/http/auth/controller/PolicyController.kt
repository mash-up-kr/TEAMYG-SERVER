package parfait.http.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.auth.port.`in`.PolicyQueryUseCase
import parfait.http.auth.dto.PolicyItemResponse
import parfait.http.auth.dto.PolicyResponse

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/policies")
class PolicyController(
    private val policyQueryUseCase: PolicyQueryUseCase,
) {
    @Operation(summary = "약관 목록 조회")
    @GetMapping
    fun getPolicies(): ApiResponse<PolicyResponse> {
        val policies =
            policyQueryUseCase.getPolicies().map {
                PolicyItemResponse(
                    termsId = it.id,
                    type = it.type.name,
                    title = it.title,
                    url = it.url,
                    required = it.required,
                )
            }
        return ApiResponse.ok(PolicyResponse(policies))
    }
}
