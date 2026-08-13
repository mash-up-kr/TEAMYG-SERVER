package parfait.core.parfait.domain

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

    companion object {
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
