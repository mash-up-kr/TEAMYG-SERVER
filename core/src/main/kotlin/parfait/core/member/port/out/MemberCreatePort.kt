package parfait.core.member.port.out

import parfait.core.auth.domain.LoginProvider

interface MemberCreatePort {
    fun create(
        provider: LoginProvider,
        providerUserId: String,
        nickname: String,
    ): Long
}
