package parfait.core.parfaittext.port.out

import parfait.core.parfaittext.domain.ParfaitText

interface ParfaitTextQueryPort {
    fun findById(parfaitTextId: Long): ParfaitText?

    fun findAllByIds(parfaitTextIds: List<Long>): List<ParfaitText>

    fun findAllByParfaitId(parfaitId: Long): List<ParfaitText>

    fun existsByParfaitId(parfaitId: Long): Boolean
}
