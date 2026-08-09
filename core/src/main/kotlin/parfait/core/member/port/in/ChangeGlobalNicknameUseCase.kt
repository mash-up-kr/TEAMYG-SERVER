@file:Suppress("ktlint:standard:package-name")

package parfait.core.member.port.`in`

interface ChangeGlobalNicknameUseCase {
    fun change(
        memberId: Long,
        nickname: String,
    ): ChangeGlobalNicknameResult
}

data class ChangeGlobalNicknameResult(
    val nickname: String,
)
