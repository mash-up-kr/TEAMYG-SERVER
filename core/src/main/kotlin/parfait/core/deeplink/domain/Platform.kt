package parfait.core.deeplink.domain

enum class Platform {
    ANDROID,
    IOS,
    OTHER,
    ;

    companion object {
        private val ANDROID_PATTERN = Regex("android", RegexOption.IGNORE_CASE)
        private val IOS_PATTERN = Regex("iphone|ipad|ipod", RegexOption.IGNORE_CASE)

        fun fromUserAgent(userAgent: String?): Platform =
            when {
                userAgent == null -> OTHER
                ANDROID_PATTERN.containsMatchIn(userAgent) -> ANDROID
                IOS_PATTERN.containsMatchIn(userAgent) -> IOS
                else -> OTHER
            }
    }
}
