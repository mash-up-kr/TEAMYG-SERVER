@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

interface CreateParfaitGroupUseCase {
    fun create(command: CreateParfaitGroupCommand): CreateParfaitGroupResult
}

data class CreateParfaitGroupCommand(
    val memberId: Long,
    val groupName: String,
    val groupNickname: String,
    val memberLimit: Int,
)

data class CreateParfaitGroupResult(
    val groupId: Long,
    val groupName: String,
    val inviteCode: String,
    val memberLimit: Int,
)
