package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.persistence.entity.ParfaitGroupMember

interface ParfaitGroupMemberRepository : JpaRepository<ParfaitGroupMember, Long>
