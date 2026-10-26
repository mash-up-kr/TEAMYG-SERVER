package parfait.core.parfaittext.domain

import parfait.core.exception.BusinessException
import parfait.core.parfaittext.exception.ParfaitTextErrorCode
import java.time.LocalDateTime

class ParfaitText private constructor(
    val id: Long?,
    val parfaitId: Long,
    val placedByGroupMemberId: Long,
    val text: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val color: String,
    val fontSize: Double,
    val isFill: Boolean,
    val isStroke: Boolean,
    val textAlign: TextAlign,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun requireId(): Long = requireNotNull(id) { "저장된 텍스트 배치의 id가 필요합니다" }

    fun update(
        positionX: Double? = null,
        positionY: Double? = null,
        positionZ: Int? = null,
        scale: Double? = null,
        rotation: Double? = null,
        color: String? = null,
        fontSize: Double? = null,
        isFill: Boolean? = null,
        isStroke: Boolean? = null,
        textAlign: TextAlign? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ): ParfaitText {
        val updatedPositionX = positionX ?: this.positionX
        val updatedPositionY = positionY ?: this.positionY
        val updatedScale = scale ?: this.scale
        val updatedRotation = rotation ?: this.rotation
        val updatedColor = color ?: this.color
        val updatedFontSize = fontSize ?: this.fontSize
        validate(
            text,
            updatedPositionX,
            updatedPositionY,
            updatedScale,
            updatedRotation,
            updatedColor,
            updatedFontSize,
        )
        return ParfaitText(
            id = id,
            parfaitId = parfaitId,
            placedByGroupMemberId = placedByGroupMemberId,
            text = text,
            positionX = updatedPositionX,
            positionY = updatedPositionY,
            positionZ = positionZ ?: this.positionZ,
            scale = updatedScale,
            rotation = updatedRotation,
            color = updatedColor,
            fontSize = updatedFontSize,
            isFill = isFill ?: this.isFill,
            isStroke = isStroke ?: this.isStroke,
            textAlign = textAlign ?: this.textAlign,
            createdAt = createdAt,
            updatedAt = now,
        )
    }

    companion object {
        fun place(
            parfaitId: Long,
            placedByGroupMemberId: Long,
            text: String,
            positionX: Double,
            positionY: Double,
            positionZ: Int,
            scale: Double,
            rotation: Double,
            color: String,
            fontSize: Double,
            isFill: Boolean = false,
            isStroke: Boolean = false,
            textAlign: TextAlign = TextAlign.CENTER,
            now: LocalDateTime = LocalDateTime.now(),
        ): ParfaitText {
            validate(text, positionX, positionY, scale, rotation, color, fontSize)
            return ParfaitText(
                id = null,
                parfaitId = parfaitId,
                placedByGroupMemberId = placedByGroupMemberId,
                text = text,
                positionX = positionX,
                positionY = positionY,
                positionZ = positionZ,
                scale = scale,
                rotation = rotation,
                color = color,
                fontSize = fontSize,
                isFill = isFill,
                isStroke = isStroke,
                textAlign = textAlign,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun reconstitute(
            id: Long,
            parfaitId: Long,
            placedByGroupMemberId: Long,
            text: String,
            positionX: Double,
            positionY: Double,
            positionZ: Int,
            scale: Double,
            rotation: Double,
            color: String,
            fontSize: Double,
            isFill: Boolean = false,
            isStroke: Boolean = false,
            textAlign: TextAlign = TextAlign.CENTER,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
        ): ParfaitText =
            ParfaitText(
                id = id,
                parfaitId = parfaitId,
                placedByGroupMemberId = placedByGroupMemberId,
                text = text,
                positionX = positionX,
                positionY = positionY,
                positionZ = positionZ,
                scale = scale,
                rotation = rotation,
                color = color,
                fontSize = fontSize,
                isFill = isFill,
                isStroke = isStroke,
                textAlign = textAlign,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun validate(
            text: String,
            positionX: Double,
            positionY: Double,
            scale: Double,
            rotation: Double,
            color: String,
            fontSize: Double,
        ) {
            if (text.isBlank() || text.length > 2000) {
                throw BusinessException(ParfaitTextErrorCode.INVALID_TEXT)
            }
            if (
                color.isBlank() ||
                color.length > 20 ||
                !positionX.isFinite() ||
                !positionY.isFinite() ||
                !rotation.isFinite() ||
                !scale.isFinite() ||
                scale <= 0 ||
                !fontSize.isFinite() ||
                fontSize <= 0
            ) {
                throw BusinessException(ParfaitTextErrorCode.INVALID_TEXT_STYLE)
            }
        }
    }
}
