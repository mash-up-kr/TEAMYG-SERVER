package parfait.core.auth.port.out

import parfait.core.auth.domain.TosType

interface TosQueryPort {
    fun findCurrentTerms(): List<CurrentTerms>
}

data class CurrentTerms(
    val id: Long,
    val type: TosType,
    val title: String,
    val url: String,
    val required: Boolean,
)
