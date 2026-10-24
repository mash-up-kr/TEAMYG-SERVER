@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaittext.port.`in`

interface DeleteParfaitTextUseCase {
    fun delete(command: DeleteParfaitTextCommand)
}

data class DeleteParfaitTextCommand(
    val memberId: Long,
    val groupId: Long,
    val parfaitId: Long,
    val parfaitTextId: Long,
)
