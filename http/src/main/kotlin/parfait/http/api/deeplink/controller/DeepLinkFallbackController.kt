package parfait.http.api.deeplink.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import parfait.core.deeplink.port.`in`.GetDownloadStoreLinkCommand
import parfait.core.deeplink.port.`in`.GetDownloadStoreLinkUseCase
import parfait.http.api.deeplink.view.DeepLinkFallbackPageRenderer
import java.nio.charset.StandardCharsets

@Tag(name = "DeepLink")
@RestController
class DeepLinkFallbackController(
    private val getDownloadStoreLinkUseCase: GetDownloadStoreLinkUseCase,
) {
    @Operation(summary = "딥링크 웹 폴백 페이지 — UA로 스토어 분기 후 리다이렉트")
    @GetMapping("/link", produces = [MediaType.TEXT_HTML_VALUE])
    fun fallback(
        @RequestHeader(HttpHeaders.USER_AGENT, required = false) userAgent: String?,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        val result =
            getDownloadStoreLinkUseCase.getDownloadLink(
                GetDownloadStoreLinkCommand(
                    userAgent = userAgent,
                    originalQueryString = request.queryString,
                ),
            )

        val html =
            DeepLinkFallbackPageRenderer.render(
                platform = result.platform,
                redirectUrl = result.redirectUrl,
            )

        return ResponseEntity
            .ok()
            .contentType(MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
            .body(html)
    }
}
