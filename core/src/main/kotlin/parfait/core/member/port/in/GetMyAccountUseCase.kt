@file:Suppress("ktlint:standard:package-name")

package parfait.core.member.port.`in`

import parfait.core.auth.domain.LoginProvider

interface GetMyAccountUseCase {
    fun getMyAccount(memberId: Long): MyAccountResult
}

data class MyAccountResult(
    val memberId: Long,
    val provider: LoginProvider,
    val nickname: String,
)
