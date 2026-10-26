@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaittext.port.`in`

import parfait.core.parfait.port.`in`.TodayParfaitTextResult
import parfait.core.parfaittext.domain.TextAlign

interface PlaceParfaitTextUseCase {
    fun place(command: PlaceParfaitTextCommand): TodayParfaitTextResult
}

data class PlaceParfaitTextCommand(
    val memberId: Long,
    val groupId: Long,
    val parfaitId: Long,
    val text: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val color: String,
    val fontSize: Double,
    val isFill: Boolean = false,
    val isStroke: Boolean = false,
    val textAlign: TextAlign = TextAlign.CENTER,
)
