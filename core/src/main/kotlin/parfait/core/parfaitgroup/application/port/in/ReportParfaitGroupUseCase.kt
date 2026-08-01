@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

interface ReportParfaitGroupUseCase {
    fun report(command: ReportParfaitGroupCommand): ReportParfaitGroupResult
}

data class ReportParfaitGroupCommand(
    val memberId: Long,
    val groupId: Long,
    val reason: String,
)

data class ReportParfaitGroupResult(
    val groupId: Long,
    val reportId: Long,
)
