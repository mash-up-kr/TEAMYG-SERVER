package parfait.core.parfaittext.port.out

import parfait.core.parfaittext.domain.ParfaitText

interface ParfaitTextQueryPort {
    fun findAllByParfaitId(parfaitId: Long): List<ParfaitText>

    fun existsByParfaitId(parfaitId: Long): Boolean
}
