@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitimage.port.`in`

interface DeleteParfaitImageUseCase {
    fun delete(command: DeleteParfaitImageCommand)
}

data class DeleteParfaitImageCommand(
    val memberId: Long,
    val groupId: Long,
    val parfaitId: Long,
    val parfaitImageId: Long,
)
