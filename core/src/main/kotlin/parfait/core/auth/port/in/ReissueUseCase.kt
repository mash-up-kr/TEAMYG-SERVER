@file:Suppress("ktlint:standard:package-name")

package parfait.core.auth.port.`in`

interface ReissueUseCase {
    fun reissue(refreshToken: String): ReissueResult
}

data class ReissueResult(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
