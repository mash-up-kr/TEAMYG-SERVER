package parfait.core.notification.domain

import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

/** P-01 문구 조립. 카피·data 키 스키마를 이 한 곳에서 관리한다. 발송 시점(디스패처)에 호출. */
@Component
class NotificationMessageFactory {
    fun toppingPlaced(
        groupName: String,
        actorNickname: String?,
        groupId: Long,
        parfaitDate: LocalDate,
    ): PushMessage =
        PushMessage(
            title = "$groupName 파르페에 체리 얹을 타이밍!",
            body = actorNickname?.let { "${it}님이 새 토핑을 쌓았어요" } ?: "누군가 새 토핑을 쌓았어요",
            data =
                mapOf(
                    "type" to "TOPPING",
                    "route" to "canvas",
                    "groupId" to groupId.toString(),
                    "date" to parfaitDate.toString(),
                ),
            ttl = Duration.ofHours(6),
        )
}
