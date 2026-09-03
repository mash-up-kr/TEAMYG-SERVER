package parfait.core.notification.port.out

import parfait.core.notification.domain.NotificationOutbox

interface NotificationOutboxAppendPort {
    /**
     * place() 트랜잭션 안에서 수신자당 1행 저장. 이미 큐잉된 dedup_key 는 pre-filter 로 걸러
     * INSERT 하지 않는다(생산자 멱등). 같은 dedup_key 로 동시 저장이 겹치는 드문 레이스에서는
     * 나중 트랜잭션이 UNIQUE 위반으로 롤백되고 상위(토핑 등록)가 재시도한다 — at-least-once.
     */
    fun saveAll(messages: List<NotificationOutbox>)
}
