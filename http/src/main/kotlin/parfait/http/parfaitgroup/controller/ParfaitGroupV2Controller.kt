package parfait.http.parfaitgroup.controller

import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfaitgroup.application.port.`in`.GetMyParfaitGroupsUseCase
import parfait.http.parfaitgroup.dto.MyParfaitGroupV2Response

/**
 * v1 내 그룹 목록은 이미지만 최근 토핑으로 본다. v2 는 텍스트까지 포함한 가장 최근 토핑을 기준으로 하고
 * 그 종류를 `recentToppingType`으로 내려준다.
 */
@RestController
@RequestMapping("/api/v2/parfait-groups")
class ParfaitGroupV2Controller(
    private val getMyParfaitGroupsUseCase: GetMyParfaitGroupsUseCase,
) {
    @GetMapping
    fun getMyGroups(authentication: Authentication): ApiResponse<List<MyParfaitGroupV2Response>> =
        ApiResponse.ok(
            getMyParfaitGroupsUseCase
                .getAllIncludingTexts(authentication.name.toLong())
                .map(MyParfaitGroupV2Response::from),
        )
}
