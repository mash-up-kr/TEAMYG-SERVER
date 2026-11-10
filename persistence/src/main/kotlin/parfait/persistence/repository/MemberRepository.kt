package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByLoginProviderAndProviderUserId(
        loginProvider: LoginProvider,
        providerUserId: String,
    ): Member?

    /** null 인자는 해당 컬럼을 바꾸지 않는다(컬럼별 원자적 UPDATE 로 동시 저장 시 lost update 방지). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "UPDATE Member m SET m.dailyReminderEnabled = COALESCE(:daily, m.dailyReminderEnabled), " +
            "m.activityDigestEnabled = COALESCE(:digest, m.activityDigestEnabled) WHERE m.id = :id",
    )
    fun updateNotificationSettings(
        @Param("id") id: Long,
        @Param("daily") daily: Boolean?,
        @Param("digest") digest: Boolean?,
    ): Int
}
