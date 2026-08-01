package parfait.core.port.out

interface MemberQueryPort {
    fun existsById(memberId: Long): Boolean

    fun findGlobalNicknameById(memberId: Long): String?
}
