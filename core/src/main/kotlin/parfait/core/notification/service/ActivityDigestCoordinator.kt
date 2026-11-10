package parfait.core.notification.service

import org.springframework.stereotype.Component
import parfait.core.notification.domain.ActivityDigestPayload
import parfait.core.notification.domain.NotificationOutbox
import parfait.core.notification.port.out.NotificationOutboxAppendPort
import parfait.core.notification.port.out.NotificationOutboxDigestPort
import parfait.core.notification.port.out.NotificationSettingsQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaittext.port.out.ParfaitTextQueryPort
import java.time.LocalDateTime

enum class DigestDecision { SEND_NOW, DEFER }

/**
 * 알림 모아 받기(P-06) 윈도우 판단과 재집계. NotificationOutboxDispatcher 의 @Transactional 안에서 호출된다.
 * 윈도우 상태는 따로 저장하지 않고 outbox 의 PENDING 인 ACTIVITY_DIGEST 예약 행이 대신한다.
 */
@Component
class ActivityDigestCoordinator(
    private val settingsQueryPort: NotificationSettingsQueryPort,
    private val digestPort: NotificationOutboxDigestPort,
    private val appendPort: NotificationOutboxAppendPort,
    private val imageQueryPort: ParfaitImageQueryPort,
    private val textQueryPort: ParfaitTextQueryPort,
) {
    /**
     * 활동 행(P-01·P-04·P-05)을 지금 보낼지 미룰지 정한다.
     * SEND_NOW 이면서 모아 받기가 켜져 있으면 이 시점에 10분 윈도우(P-06 예약 행)를 연다.
     */
    fun decide(
        row: NotificationOutbox,
        now: LocalDateTime,
    ): DigestDecision {
        val settings = settingsQueryPort.findByMemberId(row.receiverMemberId)
        if (!settings.activityDigestEnabled) return DigestDecision.SEND_NOW

        val p = row.payload
        val openKey = digestPort.findOpenDigestKey(row.receiverMemberId, p.groupId, p.parfaitDate)
        if (openKey != null) {
            digestPort.markDeferred(requireNotNull(row.id), openKey)
            return DigestDecision.DEFER
        }

        val digestPayload =
            ActivityDigestPayload(
                groupId = p.groupId,
                parfaitId = p.parfaitId,
                parfaitDate = p.parfaitDate,
                actorMemberId = p.actorMemberId,
            )
        appendPort.saveAll(
            listOf(
                NotificationOutbox.activityDigest(
                    receiverMemberId = row.receiverMemberId,
                    payload = digestPayload,
                    windowStart = now,
                    windowEnd = NotificationOutbox.digestWindowEnd(now, p.parfaitDate),
                ),
            ),
        )
        return DigestDecision.SEND_NOW
    }

    /** 예약 행(digestRef)에 묶인 대기 행을 다시 센다. 그 사이 삭제된 사진·텍스트는 뺀다. 배경 변경은 삭제될 수 없다. */
    fun countAlive(digestRef: String): Int {
        val deferred = digestPort.findDeferred(digestRef)
        if (deferred.isEmpty()) return 0

        val toppingIds = deferred.idsOf(NotificationOutbox.EVENT_TYPE_TOPPING_PLACED)
        val textIds = deferred.idsOf(NotificationOutbox.EVENT_TYPE_TEXT_PLACED)
        val backgroundCount = deferred.count { it.eventType == NotificationOutbox.EVENT_TYPE_BACKGROUND_CHANGED }

        val aliveToppings = if (toppingIds.isEmpty()) 0 else imageQueryPort.findAllByIds(toppingIds).size
        val aliveTexts = if (textIds.isEmpty()) 0 else textQueryPort.findAllByIds(textIds).size
        return aliveToppings + aliveTexts + backgroundCount
    }

    private fun List<NotificationOutbox>.idsOf(eventType: String): List<Long> =
        filter { it.eventType == eventType }.map { it.aggregateId }
}
