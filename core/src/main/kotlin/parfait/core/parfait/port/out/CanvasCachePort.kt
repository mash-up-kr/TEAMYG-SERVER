package parfait.core.parfait.port.out

import java.time.LocalDate

/**
 * v2 today 판단에 쓰는 캐시. DB 가 원본이고 이 캐시는 사본이다. 구현은 장애·미스를 모두 "없음"(null/false)으로 돌려
 * 호출자가 DB 로 내려가게 해야 하고, 쓰기·삭제 실패는 예외를 던지지 않고 삼켜야 한다.
 * 값을 덮어쓰는 `put` 은 DB 에서 막 읽은 값을 채울 때만 쓰고, 변경 반영은 항상 `evict`(삭제)로 한다.
 */
interface CanvasCachePort {
    fun getParfaitVersion(parfaitId: Long): Long?

    fun putParfaitVersion(
        parfaitId: Long,
        version: Long,
    )

    fun evictParfaitVersion(parfaitId: Long)

    fun getGroupVersion(groupId: Long): Long?

    fun putGroupVersion(
        groupId: Long,
        version: Long,
    )

    fun evictGroupVersion(groupId: Long)

    fun getTodayParfaitId(
        groupId: Long,
        date: LocalDate,
    ): Long?

    fun putTodayParfaitId(
        groupId: Long,
        date: LocalDate,
        parfaitId: Long,
    )

    /** DB 에 없는 parfaitId 를 가리키는 오래된 값(DB 복원·초기화 후)을 지울 때만 쓴다. */
    fun evictTodayParfaitId(
        groupId: Long,
        date: LocalDate,
    )

    /** 멤버라는 긍정 결과만 저장한다. 멤버가 아니라는 결과는 저장하지 않는다. */
    fun isMember(
        groupId: Long,
        memberId: Long,
    ): Boolean

    fun putMember(
        groupId: Long,
        memberId: Long,
    )

    fun evictMember(
        groupId: Long,
        memberId: Long,
    )
}
