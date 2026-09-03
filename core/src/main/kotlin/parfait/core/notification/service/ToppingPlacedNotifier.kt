package parfait.core.notification.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import parfait.core.notification.domain.NotificationOutbox
import parfait.core.notification.domain.ToppingPlacedPayload
import parfait.core.notification.event.ToppingPlacedEvent
import parfait.core.notification.port.out.NotificationOutboxAppendPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import java.time.LocalDateTime

/**
 * PlaceParfaitImageService.place() 트랜잭션 안에서 호출된다. 팬아웃(수신자당 outbox 1행) + 이벤트 발행만.
 * 문구 렌더·재검증은 발송 시점(NotificationOutboxDispatcher)에서 한다.
 */
@Service
class ToppingPlacedNotifier(
    private val groupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val appendPort: NotificationOutboxAppendPort,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun notify(
        payload: ToppingPlacedPayload,
        toppingId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        val receivers =
            groupMemberQueryPort
                .findAllByGroupId(payload.groupId)
                .filter { it.leftAt == null && it.memberId != payload.actorMemberId } // E-01
        if (receivers.isEmpty()) return

        appendPort.saveAll(
            receivers.map {
                NotificationOutbox.toppingPlaced(toppingId, it.memberId, payload, now)
            },
        )
        eventPublisher.publishEvent(ToppingPlacedEvent(toppingId))
    }
}
