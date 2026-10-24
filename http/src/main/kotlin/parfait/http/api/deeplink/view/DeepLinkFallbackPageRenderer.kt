package parfait.http.api.deeplink.view

import parfait.core.deeplink.domain.Platform
import tools.jackson.databind.ObjectMapper

object DeepLinkFallbackPageRenderer {
    private val objectMapper = ObjectMapper()

    fun render(
        platform: Platform,
        redirectUrl: String?,
        ogImageUrl: String,
    ): String {
        val pageParams = buildPageParams(platform, redirectUrl)
        val paramsJson = escapeForInlineScript(objectMapper.writeValueAsString(pageParams))
        val fallbackHref = redirectUrl?.let(::escapeHtmlAttribute) ?: "#"

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
            |    @font-face {
            |      font-family: SUIT;
            |      font-weight: 400;
            |      font-display: swap;
            |      src: url("/assets/fonts/suit-regular.woff2") format("woff2");
            |    }
            |    @font-face {
            |      font-family: SUIT;
            |      font-weight: 600;
            |      font-display: swap;
            |      src: url("/assets/fonts/suit-semibold.woff2") format("woff2");
            |    }
            |    * {
            |      box-sizing: border-box;
            |    }
            |    body {
            |      display: flex;
            |      align-items: center;
            |      justify-content: center;
            |      min-height: 100vh;
            |      margin: 0;
            |      background: #FAFAFA;
            |      font-family: SUIT, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            |      letter-spacing: -0.011em;
            |      text-align: center;
            |    }
            |    .content {
            |      display: flex;
            |      flex-direction: column;
            |      align-items: center;
            |    }
            |    .loader {
            |      display: flex;
            |      align-items: center;
            |      justify-content: center;
            |      gap: 4px;
            |      width: 44px;
            |      height: 44px;
            |      border-radius: 50%;
            |      background: rgba(14, 14, 14, 0.05);
            |    }
            |    .dot {
            |      width: 4px;
            |      height: 4px;
            |      border-radius: 50%;
            |      background: #AAACB2;
            |      animation: blink 1.2s ease-in-out infinite;
            |    }
            |    .dot:nth-child(2) {
            |      animation-delay: 0.2s;
            |    }
            |    .dot:nth-child(3) {
            |      animation-delay: 0.4s;
            |    }
            |    @keyframes blink {
            |      0%, 80%, 100% { opacity: 0.2; }
            |      40% { opacity: 1; }
            |    }
            |    @media (prefers-reduced-motion: reduce) {
            |      .dot { animation: none; }
            |    }
            |    .title {
            |      margin: 16px 0 0;
            |      font-size: 18px;
            |      font-weight: 600;
            |      line-height: 140%;
            |      color: #29292C;
            |    }
            |    .description {
            |      margin: 2px 0 0;
            |      font-size: 14px;
            |      font-weight: 400;
            |      line-height: 150%;
            |      color: #7A7D82;
            |    }
            |    .button {
            |      display: flex;
            |      align-items: center;
            |      justify-content: center;
            |      gap: 4px;
            |      width: 161.5px;
            |      height: 48px;
            |      margin-top: 24px;
            |      padding: 12px 10px;
            |      border-radius: 0;
            |      background: #29292C;
            |      font-size: 16px;
            |      font-weight: 600;
            |      line-height: 150%;
            |      color: #FAFAFA;
            |      text-decoration: none;
            |    }
            |  </style>
            |</head>
            |<body>
            |  <main class="content">
            |    <div class="loader" aria-hidden="true">
            |      <span class="dot"></span>
            |      <span class="dot"></span>
            |      <span class="dot"></span>
            |    </div>
            |    <p class="title">파르페로 이동 중입니다</p>
            |    <p class="description">잠시만 기다려 주세요...</p>
            |    <a id="fallback-link" class="button" href="$fallbackHref">직접 이동하기</a>
            |  </main>
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
