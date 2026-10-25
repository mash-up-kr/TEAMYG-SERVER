package parfait.http.parfaittext.dto

import com.fasterxml.jackson.annotation.JsonProperty
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextResult

data class UpdateParfaitTextResponse(
    val parfaitTextId: Long,
    val text: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val color: String,
    val fontSize: Double,
    @get:JsonProperty("isFill")
    val isFill: Boolean,
    @get:JsonProperty("isStroke")
    val isStroke: Boolean,
) {
    companion object {
        fun from(result: UpdateParfaitTextResult): UpdateParfaitTextResponse =
            UpdateParfaitTextResponse(
                parfaitTextId = result.parfaitTextId,
                text = result.text,
                positionX = result.positionX,
                positionY = result.positionY,
                positionZ = result.positionZ,
                scale = result.scale,
                rotation = result.rotation,
                color = result.color,
                fontSize = result.fontSize,
                isFill = result.isFill,
                isStroke = result.isStroke,
            )
    }
}
