package parfait.core.parfaitgroup.application.port.out

import parfait.core.parfaitgroup.domain.ParfaitGroup

interface ParfaitGroupSavePort {
    fun save(group: ParfaitGroup): ParfaitGroup
}
