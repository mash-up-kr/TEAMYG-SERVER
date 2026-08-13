package parfait.core.parfait.port.out

import parfait.core.parfait.domain.Parfait

interface ParfaitSavePort {
    fun save(parfait: Parfait): Parfait
}
