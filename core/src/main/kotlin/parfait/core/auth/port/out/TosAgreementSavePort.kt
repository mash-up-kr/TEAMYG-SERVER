package parfait.core.auth.port.out

interface TosAgreementSavePort {
    fun saveAll(
        memberId: Long,
        tosIds: List<Long>,
    )
}
