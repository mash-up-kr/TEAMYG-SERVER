package parfait.http.api.appversion.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.appversion.domain.AppPlatform
import parfait.core.appversion.port.`in`.CheckAppVersionUseCase
import parfait.http.api.appversion.dto.AppVersionCheckResponse

@Tag(name = "AppVersion")
@RestController
@RequestMapping("/api/v1/app-versions")
class AppVersionController(
    private val checkAppVersionUseCase: CheckAppVersionUseCase,
) {
    @Operation(summary = "앱 버전 체크 (platform: IOS 또는 ANDROID, version: x.y.z 형식)")
    @GetMapping("/check")
    fun check(
        @RequestParam platform: AppPlatform,
        @RequestParam version: String,
    ): ApiResponse<AppVersionCheckResponse> =
        ApiResponse.ok(AppVersionCheckResponse.from(checkAppVersionUseCase.check(platform, version)))
}
