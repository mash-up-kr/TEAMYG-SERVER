package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.persistence.entity.DeviceToken

interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {
    fun findByToken(token: String): DeviceToken?

    fun deleteByMemberIdAndSessionId(
        memberId: Long,
        sessionId: String,
    ): Long

    fun deleteByMemberId(memberId: Long): Long
}
