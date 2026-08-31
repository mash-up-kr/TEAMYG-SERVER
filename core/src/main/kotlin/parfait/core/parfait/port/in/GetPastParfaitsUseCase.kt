@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfait.port.`in`

import parfait.core.parfait.domain.ParfaitStatus
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
    val status: ParfaitStatus,
    val thumbnailUrl: String?,
    val imageCount: Int,
)
