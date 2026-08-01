package parfait.core.member.port.out

import parfait.core.auth.domain.LoginProvider

interface MemberQueryPort {
    fun existsById(memberId: Long): Boolean

    fun findMemberIdByProvider(
        provider: LoginProvider,
        providerUserId: String,
    ): Long?
}
