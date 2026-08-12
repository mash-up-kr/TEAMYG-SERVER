@file:Suppress("ktlint:standard:package-name")

package parfait.core.member.port.`in`

interface WithdrawUseCase {
    fun withdraw(memberId: Long)
}
