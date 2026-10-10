package parfait.persistence.cache

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import parfait.core.parfait.event.ParfaitVersionBumpedEvent
import parfait.core.parfait.port.out.CanvasCachePort
import parfait.core.parfaitgroup.application.event.GroupMemberLeftEvent
import parfait.core.parfaitgroup.application.event.GroupVersionBumpedEvent

/**
 * 쓰기 트랜잭션이 커밋된 뒤에만 캐시 키를 지운다. 커밋 전에 지우면 그 사이 다른 요청이 옛 값을 다시 캐시에 넣을 수 있다.
 * 값을 새로 쓰지 않고 지우기만 하므로 반영 순서가 뒤집혀도 결과는 "비어 있음"으로 같다.
 */
@Component
class CanvasCacheEvictionListener(
    private val cache: CanvasCachePort,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ParfaitVersionBumpedEvent) {
        cache.evictParfaitVersion(event.parfaitId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: GroupVersionBumpedEvent) {
        cache.evictGroupVersion(event.groupId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: GroupMemberLeftEvent) {
        cache.evictMember(event.groupId, event.memberId)
    }
}
