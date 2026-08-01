package parfait.persistence.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import parfait.persistence.entity.ParfaitGroup

interface ParfaitGroupRepository : JpaRepository<ParfaitGroup, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pg from ParfaitGroup pg where pg.id = :groupId")
    fun findByIdForUpdate(
        @Param("groupId") groupId: Long,
    ): ParfaitGroup?

    fun findByInviteCode(inviteCode: String): ParfaitGroup?

    fun existsByInviteCode(inviteCode: String): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pg from ParfaitGroup pg where pg.inviteCode = :inviteCode")
    fun findByInviteCodeForUpdate(
        @Param("inviteCode") inviteCode: String,
    ): ParfaitGroup?
}
