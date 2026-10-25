package parfait.core.parfaittext.port.out

import parfait.core.parfaittext.domain.ParfaitText

interface ParfaitTextSavePort {
    fun save(parfaitText: ParfaitText): ParfaitText

    fun saveAll(parfaitTexts: List<ParfaitText>): List<ParfaitText>
}
