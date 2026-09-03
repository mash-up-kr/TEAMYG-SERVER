package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import parfait.persistence.entity.DeviceToken

interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {
    fun findByToken(token: String): DeviceToken?

    fun deleteByMemberIdAndSessionId(
        memberId: Long,
        sessionId: String,
    ): Long

    fun deleteByMemberId(memberId: Long): Long

    fun findByMemberId(memberId: Long): List<DeviceToken>

    fun deleteByToken(token: String): Long

    fun deleteByTokenIn(tokens: Collection<String>): Long

    @Query(
        value = """
            SELECT dt.token
            FROM device_token dt
            WHERE EXISTS (
                SELECT 1 FROM parfait_group_member pgm
                WHERE pgm.member_id = dt.member_id AND pgm.left_at IS NULL
            )
        """,
        nativeQuery = true,
    )
    fun findActiveGroupMemberTokens(): List<String>
}
