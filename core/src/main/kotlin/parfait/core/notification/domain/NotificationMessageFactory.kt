package parfait.core.notification.domain

import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

/** 알림 문구 조립. 카피·data 키 스키마를 이 한 곳에서 관리한다. 발송 시점에 호출. */
@Component
class NotificationMessageFactory {
    fun toppingPlaced(
        groupName: String,
        actorNickname: String?,
        groupId: Long,
        parfaitDate: LocalDate,
    ): PushMessage =
        PushMessage(
            title = "$groupName 파르페에 체리 하나 톡!",
            body =
                actorNickname
                    ?.ellipsize(NICKNAME_DISPLAY_MAX)
                    ?.let { "${it}님이 새 토핑을 쌓았어요" }
                    ?: "누군가 새 토핑을 쌓았어요",
            data =
                mapOf(
                    "type" to "TOPPING",
                    "route" to "canvas",
                    "groupId" to groupId.toString(),
                    "date" to parfaitDate.toString(),
                ),
            ttl = Duration.ofHours(6),
        )

    fun dailyReminder(type: ReminderType): PushMessage {
        val (title, body) =
            when (type) {
                ReminderType.MORNING -> "새벽 3시에 오늘의 새 캔버스가 열렸어요" to "오늘의 첫 토핑을 쌓아볼까요?"
                ReminderType.EVENING -> "새벽 3시에 오늘의 캔버스가 마감돼요" to "오늘의 마지막 토핑을 올리러 가볼까요?"
            }
        return PushMessage(
            title = title,
            body = body,
            data = mapOf("type" to type.dataType, "route" to "group"),
            ttl = Duration.ofHours(1),
        )
    }

    private fun String.ellipsize(max: Int): String = if (length > max) take(max) + "..." else this

    private companion object {
        const val NICKNAME_DISPLAY_MAX = 8
    }
}
