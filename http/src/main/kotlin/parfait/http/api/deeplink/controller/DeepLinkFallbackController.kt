package parfait.http.api.deeplink.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import parfait.core.deeplink.port.`in`.ResolveStoreFallbackCommand
import parfait.core.deeplink.port.`in`.ResolveStoreFallbackUseCase
import parfait.http.api.deeplink.view.DeepLinkFallbackPageRenderer

// App Link/Universal Link 인터셉트가 실패했을 때(주로 앱 미설치)만 실제로 로드되는 웹 폴백 페이지.
@Tag(name = "DeepLink")
@RestController
class DeepLinkFallbackController(
    private val resolveStoreFallbackUseCase: ResolveStoreFallbackUseCase,
    @Value("\${deeplink.ga4.measurement-id:}") private val ga4MeasurementId: String,
) {
    @Operation(summary = "딥링크 웹 폴백 페이지 — UA로 스토어 분기 후 리다이렉트")
    @GetMapping("/link", produces = [MediaType.TEXT_HTML_VALUE])
    fun fallback(
        @RequestHeader(HttpHeaders.USER_AGENT, required = false) userAgent: String?,
        @RequestParam("link_id", required = false) linkId: String?,
        @RequestParam(required = false) campaign: String?,
        @RequestParam(required = false) source: String?,
        @RequestParam(required = false) medium: String?,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        val result =
            resolveStoreFallbackUseCase.resolve(
                ResolveStoreFallbackCommand(
                    userAgent = userAgent,
                    originalQueryString = request.queryString,
                ),
            )

        val html =
            DeepLinkFallbackPageRenderer.render(
                platform = result.platform,
                redirectUrl = result.redirectUrl,
                linkId = linkId,
                campaign = campaign,
                source = source,
                medium = medium,
                ga4MeasurementId = ga4MeasurementId,
            )

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html)
    }
}
