package parfait.core.parfaitgroup.application.port.out

import parfait.core.parfaitgroup.domain.InviteCode
import parfait.core.parfaitgroup.domain.ParfaitGroup

interface ParfaitGroupQueryPort {
    fun findById(groupId: Long): ParfaitGroup?

    fun findByIdForUpdate(groupId: Long): ParfaitGroup?

    fun findByInviteCode(inviteCode: InviteCode): ParfaitGroup?

    fun findByInviteCodeForUpdate(inviteCode: InviteCode): ParfaitGroup?

    fun existsByInviteCode(inviteCode: InviteCode): Boolean

    fun findAllIds(): List<Long>
}
