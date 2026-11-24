package parfait.core.appversion.exception

import parfait.common.error.BaseErrorCode

enum class AppVersionErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    INVALID_VERSION_FORMAT(400, "INVALID_VERSION_FORMAT", "버전 형식이 올바르지 않습니다 (예: 1.4.0)"),
    APP_VERSION_NOT_FOUND(500, "APP_VERSION_NOT_FOUND", "앱 버전 정보가 설정되어 있지 않습니다"),
}
