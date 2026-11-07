package parfait.core.notification.port.out

import parfait.core.notification.domain.NotificationOutbox
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 알림 모아 받기(P-06) 전용. NotificationOutboxDispatcher 의 @Transactional 경계 안에서 호출된다.
 */
interface NotificationOutboxDigestPort {
    /** 이 (수신자, 그룹, 날짜)에 아직 발송 전(PENDING)인 P-06 예약 행이 있으면 윈도우가 열려 있는 것이다. */
    fun findOpenDigestKey(
        receiverMemberId: Long,
        groupId: Long,
        parfaitDate: LocalDate,
    ): String?

    /** 활동 행을 DEFERRED 로 바꾸고 예약 행(dedupKey)에 연결한다. */
    fun markDeferred(
        id: Long,
        digestRef: String,
    )

    /** 예약 행에 묶인 DEFERRED 행. */
    fun findDeferred(digestRef: String): List<NotificationOutbox>

    /** 묶인 DEFERRED 행을 처리 끝(SENT + note)으로 바꾼다. */
    fun resolveDeferred(
        digestRef: String,
        now: LocalDateTime,
        note: String,
    )
}
