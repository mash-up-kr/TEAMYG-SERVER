package parfait.http.api.deeplink.view

import parfait.core.deeplink.domain.Platform
import tools.jackson.databind.ObjectMapper

// 앱 미설치 상태에서 App Link/Universal Link 인터셉트가 실패했을 때만 브라우저에 실제로 로드되는
// 폴백 페이지. 리다이렉트 직전에 GA4 커스텀 이벤트를 transport_type: 'beacon'으로 전송해야
// 리다이렉트로 요청이 끊기기 전에 이벤트가 나가므로, 순수 서버 리다이렉트가 아니라 클라이언트
// 스크립트(gtag.js)로 처리한다.
object DeepLinkFallbackPageRenderer {
    private val objectMapper = ObjectMapper()

    fun render(
        platform: Platform,
        redirectUrl: String?,
        linkId: String?,
        campaign: String?,
        source: String?,
        medium: String?,
        ga4MeasurementId: String,
    ): String {
        val eventParams = buildEventParams(platform, redirectUrl, linkId, campaign, source, medium)
        val paramsJson = escapeForInlineScript(objectMapper.writeValueAsString(eventParams))
        val fallbackHref = redirectUrl?.let(::escapeHtmlAttribute) ?: "#"

        return """
            |<!doctype html>
            |<html lang="ko">
            |<head>
            |  <meta charset="utf-8" />
            |  <meta name="viewport" content="width=device-width, initial-scale=1" />
            |  <title>Parfait</title>
            |</head>
            |<body>
            |  <p>앱으로 이동 중입니다...</p>
            |  <p><a id="fallback-link" href="$fallbackHref">스토어로 이동하기</a></p>
            |${analyticsScript(ga4MeasurementId)}
            |  <script>
            |    (function () {
            |      var params = $paramsJson;
            |      var redirectUrl = params.redirect_url || null;
            |      function goToStore() {
            |        if (redirectUrl) {
            |          window.location.replace(redirectUrl);
            |        }
            |      }
            |${redirectScript(ga4MeasurementId)}
            |    })();
            |  </script>
            |</body>
            |</html>
            """.trimMargin()
    }

    private fun buildEventParams(
        platform: Platform,
        redirectUrl: String?,
        linkId: String?,
        campaign: String?,
        source: String?,
        medium: String?,
    ): Map<String, String> =
        buildMap {
            put("platform", platform.name)
            redirectUrl?.let { put("redirect_url", it) }
            linkId?.let { put("link_id", it) }
            campaign?.let { put("campaign", it) }
            source?.let { put("source", it) }
            medium?.let { put("medium", it) }
        }

    private fun analyticsScript(ga4MeasurementId: String): String {
        if (ga4MeasurementId.isBlank()) return ""
        val escapedId = escapeHtmlAttribute(ga4MeasurementId)
        val escapedIdJs = escapeForInlineScript(objectMapper.writeValueAsString(ga4MeasurementId))
        return """
            |  <script async src="https://www.googletagmanager.com/gtag/js?id=$escapedId"></script>
            |  <script>
            |    window.dataLayer = window.dataLayer || [];
            |    function gtag(){dataLayer.push(arguments);}
            |    gtag('js', new Date());
            |    gtag('config', $escapedIdJs);
            |  </script>
            """.trimMargin()
    }

    private fun redirectScript(ga4MeasurementId: String): String =
        if (ga4MeasurementId.isBlank()) {
            "      goToStore();"
        } else {
            """
            |      gtag('event', 'app_link_fallback', Object.assign({}, params, { transport_type: 'beacon', event_callback: goToStore }));
            |      setTimeout(goToStore, 1000);
            """.trimMargin()
        }

    // Jackson이 이스케이프한 문자열을 <script> 안에 그대로 넣더라도 "</script"로 조기 종료될 수
    // 있어, 슬래시가 포함된 "</" 시퀀스만 추가로 끊어준다.
    private fun escapeForInlineScript(json: String): String = json.replace("</", "<\\/")

    private fun escapeHtmlAttribute(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}
