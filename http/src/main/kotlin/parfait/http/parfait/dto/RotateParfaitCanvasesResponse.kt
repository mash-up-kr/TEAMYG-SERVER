package parfait.http.parfait.dto

data class RotateParfaitCanvasesResponse(
    val closedCount: Int,
    val emptyCount: Int,
    val createdCount: Int,
    val failedCount: Int,
)
