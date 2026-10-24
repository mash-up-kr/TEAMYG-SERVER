package parfait.core.notification.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import parfait.core.notification.domain.NotificationOutbox
import parfait.core.notification.domain.TextPlacedPayload
import parfait.core.notification.event.TextPlacedEvent
import parfait.core.notification.port.out.NotificationOutboxAppendPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import java.time.LocalDateTime

@Service
class TextPlacedNotifier(
    private val groupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val appendPort: NotificationOutboxAppendPort,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun notify(
        payload: TextPlacedPayload,
        textId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        val receivers =
            groupMemberQueryPort
                .findAllByGroupId(payload.groupId)
                .filter { it.leftAt == null && it.memberId != payload.actorMemberId }
        if (receivers.isEmpty()) return

        appendPort.saveAll(
            receivers.map {
                NotificationOutbox.textPlaced(textId, it.memberId, payload, now)
            },
        )
        eventPublisher.publishEvent(TextPlacedEvent(textId))
    }
}
