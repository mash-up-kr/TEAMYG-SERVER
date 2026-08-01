@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

interface JoinParfaitGroupUseCase {
    fun join(command: JoinParfaitGroupCommand): JoinParfaitGroupResult
}

data class JoinParfaitGroupCommand(
    val memberId: Long,
    val inviteCode: String,
)

data class JoinParfaitGroupResult(
    val groupId: Long,
    val groupName: String,
)
