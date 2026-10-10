package parfait.core.parfaitgroup.application.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import parfait.core.parfaitgroup.application.event.GroupMemberLeftEvent
import parfait.core.parfaitgroup.application.event.GroupVersionBumpedEvent
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupVersionPort

/** 멤버 목록·닉네임처럼 today 응답의 그룹 쪽 값이 바뀌는 쓰기는 같은 트랜잭션 안에서 이 서비스를 호출한다. */
@Service
class ParfaitGroupVersionService(
    private val groupVersionPort: ParfaitGroupVersionPort,
    private val eventPublisher: ApplicationEventPublisher,
) {
    // 트랜잭션 밖에서 호출되면 AFTER_COMMIT 무효화 이벤트가 조용히 버려지므로, 진행 중인 트랜잭션을 요구한다.
    @Transactional(propagation = Propagation.MANDATORY)
    fun bump(groupId: Long) {
        groupVersionPort.bump(groupId)
        eventPublisher.publishEvent(GroupVersionBumpedEvent(groupId))
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun bumpOnMemberLeft(
        groupId: Long,
        memberId: Long,
    ) {
        bump(groupId)
        eventPublisher.publishEvent(GroupMemberLeftEvent(groupId, memberId))
    }
}
