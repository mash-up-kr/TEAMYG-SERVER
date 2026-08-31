package parfait.core.parfait.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.ParfaitDay
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.GetPastParfaitsCommand
import parfait.core.parfait.port.`in`.GetPastParfaitsUseCase
import parfait.core.parfait.port.`in`.PastParfaitResult
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort

@Service
class GetPastParfaitsService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitImageQueryPort: ParfaitImageQueryPort,
) : GetPastParfaitsUseCase {
    @Transactional(readOnly = true)
    override fun getPastParfaits(command: GetPastParfaitsCommand): List<PastParfaitResult> {
        if (!parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(command.groupId, command.memberId)) {
            throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)
        }

        val to = command.to ?: ParfaitDay.current()
        val from = command.from ?: to.minusDays(DEFAULT_RANGE_DAYS)
        if (from.isAfter(to)) {
            throw BusinessException(ParfaitErrorCode.INVALID_DATE_RANGE)
        }

        val parfaits = parfaitQueryPort.findAllByGroupIdAndDateRange(command.groupId, from, to)
        if (parfaits.isEmpty()) {
            return emptyList()
        }

        val imageCounts = parfaitImageQueryPort.countAllByParfaitIds(parfaits.map { it.id })
        return parfaits.map {
            PastParfaitResult(
                parfaitId = it.id,
                date = it.date,
                status = it.status,
                thumbnailUrl = null,
                imageCount = imageCounts[it.id] ?: 0,
            )
        }
    }

    companion object {
        private const val DEFAULT_RANGE_DAYS = 30L
    }
}
