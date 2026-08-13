package parfait.http.parfait.dto

import parfait.core.parfait.port.`in`.PastParfaitResult
import java.time.LocalDate

data class PastParfaitsResponse(
    val parfaits: List<PastParfaitResponse>,
) {
    companion object {
        fun from(results: List<PastParfaitResult>): PastParfaitsResponse =
            PastParfaitsResponse(results.map(PastParfaitResponse::from))
    }
}

data class PastParfaitResponse(
    val parfaitId: Long,
    val date: LocalDate,
    val thumbnailUrl: String?,
    val imageCount: Int,
) {
    companion object {
        fun from(result: PastParfaitResult): PastParfaitResponse =
            PastParfaitResponse(
                parfaitId = result.parfaitId,
                date = result.date,
                thumbnailUrl = result.thumbnailUrl,
                imageCount = result.imageCount,
            )
    }
}
