@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

interface ChangeMyParfaitGroupNicknameUseCase {
    fun change(command: ChangeMyParfaitGroupNicknameCommand): ChangeMyParfaitGroupNicknameResult
}

data class ChangeMyParfaitGroupNicknameCommand(
    val memberId: Long,
    val groupId: Long,
    val groupNickname: String,
)

data class ChangeMyParfaitGroupNicknameResult(
    val groupId: Long,
    val groupNickname: String,
)
