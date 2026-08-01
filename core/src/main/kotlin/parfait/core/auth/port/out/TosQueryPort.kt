package parfait.core.auth.port.out

interface TosQueryPort {
    fun findCurrentTerms(): List<CurrentTerms>
}

data class CurrentTerms(
    val id: Long,
    val required: Boolean,
)
