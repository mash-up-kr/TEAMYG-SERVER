package parfait.core.notification.domain

data class TokenSendResult(
    val token: String,
    val success: Boolean,
    val errorCode: String?, // FirebaseMessagingException.messagingErrorCode?.name
)

data class MulticastResult(
    val results: List<TokenSendResult>,
) {
    val successCount: Int get() = results.count { it.success }
    val failureCount: Int get() = results.count { !it.success }

    fun deadTokens(): List<String> = results.filter { it.errorCode in FcmErrorCodes.DEAD_TOKEN }.map { it.token }
}
