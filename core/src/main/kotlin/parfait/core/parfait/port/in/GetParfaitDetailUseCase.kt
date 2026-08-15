@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfait.port.`in`

interface GetParfaitDetailUseCase {
    fun getDetail(command: GetParfaitDetailCommand): GetTodayParfaitResult
}

data class GetParfaitDetailCommand(
    val memberId: Long,
    val groupId: Long,
    val parfaitId: Long,
)
