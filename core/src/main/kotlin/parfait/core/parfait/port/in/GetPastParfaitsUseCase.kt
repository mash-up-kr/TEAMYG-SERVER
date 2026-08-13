@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfait.port.`in`

import java.time.LocalDate

interface GetPastParfaitsUseCase {
    fun getPastParfaits(command: GetPastParfaitsCommand): List<PastParfaitResult>
}

data class GetPastParfaitsCommand(
    val memberId: Long,
    val groupId: Long,
    val from: LocalDate?,
    val to: LocalDate?,
)

data class PastParfaitResult(
    val parfaitId: Long,
    val date: LocalDate,
    val thumbnailUrl: String?,
    val imageCount: Int,
)
