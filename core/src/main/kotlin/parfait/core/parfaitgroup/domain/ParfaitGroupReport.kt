package parfait.core.parfaitgroup.domain

import java.time.LocalDateTime

class ParfaitGroupReport private constructor(
    val id: Long?,
    val parfaitGroupId: Long,
    val reporterMemberId: Long,
    val reason: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun create(
            parfaitGroupId: Long,
            reporterMemberId: Long,
            reason: String,
            createdAt: LocalDateTime = LocalDateTime.now(),
        ): ParfaitGroupReport {
            if (reason.isBlank()) {
                throw ParfaitGroupException(ParfaitGroupError.INVALID_GROUP_REPORT_REASON)
            }
            return ParfaitGroupReport(
                id = null,
                parfaitGroupId = parfaitGroupId,
                reporterMemberId = reporterMemberId,
                reason = reason,
                createdAt = createdAt,
            )
        }

        fun reconstitute(
            id: Long,
            parfaitGroupId: Long,
            reporterMemberId: Long,
            reason: String,
            createdAt: LocalDateTime,
        ): ParfaitGroupReport =
            ParfaitGroupReport(
                id = id,
                parfaitGroupId = parfaitGroupId,
                reporterMemberId = reporterMemberId,
                reason = reason,
                createdAt = createdAt,
            )
    }
}
