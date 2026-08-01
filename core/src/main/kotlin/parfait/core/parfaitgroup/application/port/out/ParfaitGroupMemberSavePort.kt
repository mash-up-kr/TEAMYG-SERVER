package parfait.core.parfaitgroup.application.port.out

import parfait.core.parfaitgroup.domain.ParfaitGroupMember

interface ParfaitGroupMemberSavePort {
    fun save(groupMember: ParfaitGroupMember): ParfaitGroupMember
}
