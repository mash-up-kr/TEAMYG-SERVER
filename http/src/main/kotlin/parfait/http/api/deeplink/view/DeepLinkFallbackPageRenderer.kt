package parfait.http.api.deeplink.view

import parfait.core.deeplink.domain.Platform
import tools.jackson.databind.ObjectMapper

// 순수 서버 리다이렉트 대신 클라이언트 스크립트를 쓰는 이유: GA4 beacon 이벤트가 리다이렉트로
// 요청이 끊기기 전에 나가야 하기 때문이다.
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
            |  <style>
            |    body {
            |      display: flex;
            |      flex-direction: column;
            |      align-items: center;
            |      justify-content: center;
            |      gap: 16px;
            |      height: 100vh;
            |      margin: 0;
            |      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            |      color: #333;
            |      text-align: center;
            |    }
            |    .spinner {
            |      width: 28px;
            |      height: 28px;
            |      border: 3px solid #eee;
            |      border-top-color: #ff6b6b;
            |      border-radius: 50%;
            |      animation: spin 0.8s linear infinite;
            |    }
            |    @keyframes spin {
            |      to { transform: rotate(360deg); }
            |    }
            |    a { color: #ff6b6b; }
            |  </style>
            |</head>
            |<body>
            |  <div class="spinner"></div>
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

    // "</script"로 조기 종료되는 것을 막기 위해 "</" 시퀀스를 끊는다.
    private fun escapeForInlineScript(json: String): String = json.replace("</", "<\\/")

    private fun escapeHtmlAttribute(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}
