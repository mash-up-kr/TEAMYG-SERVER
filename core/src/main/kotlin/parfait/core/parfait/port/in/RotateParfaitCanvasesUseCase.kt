@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfait.port.`in`

interface RotateParfaitCanvasesUseCase {
    fun rotateAll(): RotateParfaitCanvasesResult
}

data class RotateParfaitCanvasesResult(
    val closedCount: Int,
    val emptyCount: Int,
    val createdCount: Int,
    val failedCount: Int,
)
