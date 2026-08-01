package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByLoginProviderAndProviderUserId(
        loginProvider: LoginProvider,
        providerUserId: String,
    ): Member?
}
