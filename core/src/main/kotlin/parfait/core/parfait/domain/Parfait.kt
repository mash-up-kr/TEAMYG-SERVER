package parfait.core.parfait.domain

import parfait.core.exception.BusinessException
import parfait.core.parfait.exception.ParfaitErrorCode
import java.time.LocalDate
import java.time.LocalDateTime

class Parfait private constructor(
    val id: Long?,
    val parfaitGroupId: Long,
    val parfaitDate: LocalDate,
    val status: ParfaitStatus,
    val backgroundType: BackgroundType?,
    val backgroundValue: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun requireId(): Long = requireNotNull(id) { "저장된 파르페의 id가 필요합니다" }

    fun close(now: LocalDateTime = LocalDateTime.now()): Parfait {
        if (status != ParfaitStatus.ACTIVE) {
            throw BusinessException(ParfaitErrorCode.PARFAIT_ALREADY_CLOSED)
        }
        return Parfait(
            id = id,
            parfaitGroupId = parfaitGroupId,
            parfaitDate = parfaitDate,
            status = ParfaitStatus.CLOSED,
            backgroundType = backgroundType,
            backgroundValue = backgroundValue,
            createdAt = createdAt,
            updatedAt = now,
        )
    }

    fun markEmpty(now: LocalDateTime = LocalDateTime.now()): Parfait {
        if (status != ParfaitStatus.ACTIVE) {
            throw BusinessException(ParfaitErrorCode.PARFAIT_ALREADY_CLOSED)
        }
        return Parfait(
            id = id,
            parfaitGroupId = parfaitGroupId,
            parfaitDate = parfaitDate,
            status = ParfaitStatus.EMPTY,
            backgroundType = backgroundType,
            backgroundValue = backgroundValue,
            createdAt = createdAt,
            updatedAt = now,
        )
    }

    fun changeBackground(
        type: BackgroundType,
        value: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): Parfait {
        validateBackground(type, value)
        return Parfait(
            id = id,
            parfaitGroupId = parfaitGroupId,
            parfaitDate = parfaitDate,
            status = status,
            backgroundType = type,
            backgroundValue = value,
            createdAt = createdAt,
            updatedAt = now,
        )
    }

    companion object {
        private val HEX_COLOR_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")

        private fun validateBackground(
            type: BackgroundType,
            value: String,
        ) {
            if (type == BackgroundType.COLOR && !HEX_COLOR_PATTERN.matches(value)) {
                throw BusinessException(ParfaitErrorCode.INVALID_BACKGROUND)
            }
        }

        fun createToday(
            parfaitGroupId: Long,
            date: LocalDate,
            now: LocalDateTime = LocalDateTime.now(),
        ): Parfait =
            Parfait(
                id = null,
                parfaitGroupId = parfaitGroupId,
                parfaitDate = date,
                status = ParfaitStatus.ACTIVE,
                backgroundType = null,
                backgroundValue = null,
                createdAt = now,
                updatedAt = now,
            )

        fun reconstitute(
            id: Long,
            parfaitGroupId: Long,
            parfaitDate: LocalDate,
            status: ParfaitStatus,
            backgroundType: BackgroundType?,
            backgroundValue: String?,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
        ): Parfait =
            Parfait(
                id = id,
                parfaitGroupId = parfaitGroupId,
                parfaitDate = parfaitDate,
                status = status,
                backgroundType = backgroundType,
                backgroundValue = backgroundValue,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }
}
