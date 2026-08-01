package parfait.core.parfait.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.parfait.port.`in`.GetParfaitYearsUseCase
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException

@Service
class ParfaitService(
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
) : GetParfaitYearsUseCase {
    @Transactional(readOnly = true)
    override fun getYears(
        memberId: Long,
        groupId: Long,
    ): List<Int> {
        if (!parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)
        }
        return parfaitQueryPort.findDistinctYearsByGroupId(groupId)
    }
}
