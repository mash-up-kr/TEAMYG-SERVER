package parfait.core.parfaitgroup.application.port.out

import parfait.core.parfaitgroup.domain.ParfaitGroupMember

interface ParfaitGroupMemberLeavePort {
    fun leave(groupMember: ParfaitGroupMember)
}
