@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

interface LeaveParfaitGroupUseCase {
    fun leave(command: LeaveParfaitGroupCommand): LeaveParfaitGroupResult
}

data class LeaveParfaitGroupCommand(
    val memberId: Long,
    val groupId: Long,
)

data class LeaveParfaitGroupResult(
    val groupId: Long,
)
