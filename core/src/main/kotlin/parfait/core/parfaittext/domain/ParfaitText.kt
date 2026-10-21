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
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun requireId(): Long = requireNotNull(id) { "저장된 텍스트 배치의 id가 필요합니다" }

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
            now: LocalDateTime = LocalDateTime.now(),
        ): ParfaitText {
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
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }
}
