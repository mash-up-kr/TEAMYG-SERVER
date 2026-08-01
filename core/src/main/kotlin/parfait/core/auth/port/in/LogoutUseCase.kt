@file:Suppress("ktlint:standard:package-name")

package parfait.core.auth.port.`in`

interface LogoutUseCase {
    fun logout(
        memberId: Long,
        refreshToken: String,
    )
}
