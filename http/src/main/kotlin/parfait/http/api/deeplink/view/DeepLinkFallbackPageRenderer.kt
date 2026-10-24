package parfait.http.api.deeplink.view

import parfait.core.deeplink.domain.Platform
import tools.jackson.databind.ObjectMapper

object DeepLinkFallbackPageRenderer {
    private val objectMapper = ObjectMapper()

    fun render(
        platform: Platform,
        redirectUrl: String?,
        ogImageUrl: String,
        isInAppBrowser: Boolean = false,
    ): String {
        val pageParams = buildPageParams(platform, redirectUrl)
        val paramsJson = escapeForInlineScript(objectMapper.writeValueAsString(pageParams))
        val fallbackHref = redirectUrl?.let(::escapeHtmlAttribute) ?: "#"
        val message = if (isInAppBrowser) "외부 브라우저로 이동 중입니다..." else "앱으로 이동 중입니다..."
        val linkLabel = if (isInAppBrowser) "외부 브라우저로 열기" else "스토어로 이동하기"

        // TODO: 디자인 적용 예정 (현재는 최소한의 스타일만 적용된 임시 페이지)
        return """
            |<!doctype html>
            |<html lang="ko">
            |<head>
            |  <meta charset="utf-8" />
            |  <meta name="viewport" content="width=device-width, initial-scale=1" />
            |  <title>Parfait</title>
            |  <meta property="og:type" content="website" />
            |  <meta property="og:title" content="Parfait" />
            |  <meta property="og:description" content="우리가 쌓을 하루, 사진 공유 캔버스 SNS" />
            |  <meta property="og:image" content="${escapeHtmlAttribute(ogImageUrl)}" />
            |  <meta property="og:image:width" content="1200" />
            |  <meta property="og:image:height" content="600" />
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
            |  <p>$message</p>
            |  <p><a id="fallback-link" href="$fallbackHref">$linkLabel</a></p>
            |  <script>
            |    (function () {
            |      var params = $paramsJson;
            |      var redirectUrl = params.redirect_url || null;
            |      function goToStore() {
            |        if (redirectUrl) {
            |          window.location.replace(redirectUrl);
            |        }
            |      }
            |      goToStore();
            |    })();
            |  </script>
            |</body>
            |</html>
            """.trimMargin()
    }

    private fun buildPageParams(
        platform: Platform,
        redirectUrl: String?,
    ): Map<String, String> =
        buildMap {
            put("platform", platform.name)
            redirectUrl?.let { put("redirect_url", it) }
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
