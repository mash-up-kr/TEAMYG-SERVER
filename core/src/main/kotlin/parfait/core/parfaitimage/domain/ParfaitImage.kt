package parfait.core.parfaitimage.domain

import parfait.core.exception.BusinessException
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import java.time.LocalDateTime

class ParfaitImage private constructor(
    val id: Long?,
    val parfaitId: Long,
    val imageMetaId: Long,
    val placedByGroupMemberId: Long,
    val imageUrl: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val borderType: BorderType,
    val borderColor: String?,
    val borderWidth: Double?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun requireId(): Long = requireNotNull(id) { "저장된 토핑 배치의 id가 필요합니다" }

    fun reposition(
        placedByGroupMemberId: Long,
        positionX: Double,
        positionY: Double,
        positionZ: Int,
        scale: Double,
        rotation: Double,
        borderType: BorderType,
        borderColor: String?,
        borderWidth: Double?,
        now: LocalDateTime = LocalDateTime.now(),
    ): ParfaitImage {
        validateBorder(borderType, borderColor, borderWidth)
        return ParfaitImage(
            id = id,
            parfaitId = parfaitId,
            imageMetaId = imageMetaId,
            placedByGroupMemberId = placedByGroupMemberId,
            imageUrl = imageUrl,
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            scale = scale,
            rotation = rotation,
            borderType = borderType,
            borderColor = borderColor,
            borderWidth = borderWidth,
            createdAt = createdAt,
            updatedAt = now,
        )
    }

    fun update(
        positionX: Double? = null,
        positionY: Double? = null,
        positionZ: Int? = null,
        scale: Double? = null,
        rotation: Double? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ): ParfaitImage =
        ParfaitImage(
            id = id,
            parfaitId = parfaitId,
            imageMetaId = imageMetaId,
            placedByGroupMemberId = placedByGroupMemberId,
            imageUrl = imageUrl,
            positionX = positionX ?: this.positionX,
            positionY = positionY ?: this.positionY,
            positionZ = positionZ ?: this.positionZ,
            scale = scale ?: this.scale,
            rotation = rotation ?: this.rotation,
            borderType = borderType,
            borderColor = borderColor,
            borderWidth = borderWidth,
            createdAt = createdAt,
            updatedAt = now,
        )

    fun updateBorder(
        borderType: BorderType,
        borderColor: String?,
        borderWidth: Double?,
        now: LocalDateTime = LocalDateTime.now(),
    ): ParfaitImage {
        validateBorder(borderType, borderColor, borderWidth)
        return ParfaitImage(
            id = id,
            parfaitId = parfaitId,
            imageMetaId = imageMetaId,
            placedByGroupMemberId = placedByGroupMemberId,
            imageUrl = imageUrl,
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            scale = scale,
            rotation = rotation,
            borderType = borderType,
            borderColor = borderColor,
            borderWidth = borderWidth,
            createdAt = createdAt,
            updatedAt = now,
        )
    }

    companion object {
        fun place(
            parfaitId: Long,
            imageMetaId: Long,
            placedByGroupMemberId: Long,
            imageUrl: String,
            positionX: Double,
            positionY: Double,
            positionZ: Int,
            scale: Double,
            rotation: Double,
            borderType: BorderType,
            borderColor: String?,
            borderWidth: Double?,
            now: LocalDateTime = LocalDateTime.now(),
        ): ParfaitImage {
            validateBorder(borderType, borderColor, borderWidth)
            return ParfaitImage(
                id = null,
                parfaitId = parfaitId,
                imageMetaId = imageMetaId,
                placedByGroupMemberId = placedByGroupMemberId,
                imageUrl = imageUrl,
                positionX = positionX,
                positionY = positionY,
                positionZ = positionZ,
                scale = scale,
                rotation = rotation,
                borderType = borderType,
                borderColor = borderColor,
                borderWidth = borderWidth,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun reconstitute(
            id: Long,
            parfaitId: Long,
            imageMetaId: Long,
            placedByGroupMemberId: Long,
            imageUrl: String,
            positionX: Double,
            positionY: Double,
            positionZ: Int,
            scale: Double,
            rotation: Double,
            borderType: BorderType,
            borderColor: String?,
            borderWidth: Double?,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
        ): ParfaitImage =
            ParfaitImage(
                id = id,
                parfaitId = parfaitId,
                imageMetaId = imageMetaId,
                placedByGroupMemberId = placedByGroupMemberId,
                imageUrl = imageUrl,
                positionX = positionX,
                positionY = positionY,
                positionZ = positionZ,
                scale = scale,
                rotation = rotation,
                borderType = borderType,
                borderColor = borderColor,
                borderWidth = borderWidth,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun validateBorder(
            borderType: BorderType,
            borderColor: String?,
            borderWidth: Double?,
        ) {
            if (borderType == BorderType.SOLID && (borderColor == null || borderWidth == null)) {
                throw BusinessException(ParfaitImageErrorCode.INVALID_BORDER)
            }
        }
    }
}
