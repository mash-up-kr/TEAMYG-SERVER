package parfait.core.member.port.out

import parfait.core.auth.domain.LoginProvider

interface MemberQueryPort {
    fun existsById(memberId: Long): Boolean

    fun findGlobalNicknameById(memberId: Long): String?

    fun findMemberIdByProvider(
        provider: LoginProvider,
        providerUserId: String,
    ): Long?

    fun findAccountById(memberId: Long): MemberAccount?
}

data class MemberAccount(
    val provider: LoginProvider,
    val nickname: String,
)
