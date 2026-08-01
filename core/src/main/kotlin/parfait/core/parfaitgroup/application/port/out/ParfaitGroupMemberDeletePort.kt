package parfait.core.parfaitgroup.application.port.out

import parfait.core.parfaitgroup.domain.ParfaitGroupMember

interface ParfaitGroupMemberDeletePort {
    fun delete(groupMember: ParfaitGroupMember)
}
