package parfait.core.deeplink.domain

/**
 * User-Agent 기반 플랫폼 구분. 각 항목이 자신의 판정 규칙([matcher])을 소유한다.
 * 판정은 선언 순서대로 진행되어 먼저 매칭되는 항목이 우선하며, 규칙이 없는 [OTHER]는 폴백이다.
 */
enum class Platform(
    private val matcher: ((String) -> Boolean)?,
) {
    ANDROID({ it.contains("android", ignoreCase = true) }),
    IOS({ ua -> IOS_KEYWORDS.any { ua.contains(it, ignoreCase = true) } }),
    OTHER(null),
    ;

    companion object {
        private val IOS_KEYWORDS = listOf("iphone", "ipad", "ipod")

        fun fromUserAgent(userAgent: String?): Platform =
            userAgent?.let { ua -> entries.firstOrNull { it.matcher?.invoke(ua) == true } } ?: OTHER
    }
}
