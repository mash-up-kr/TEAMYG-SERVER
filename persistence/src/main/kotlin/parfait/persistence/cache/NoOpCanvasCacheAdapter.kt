package parfait.persistence.cache

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import parfait.core.parfait.port.out.CanvasCachePort
import java.time.LocalDate

/** `parfait.canvas-cache.enabled=false` 일 때 쓰는 캐시. 항상 miss 이고 아무것도 저장하지 않아 Redis 를 전혀 호출하지 않는다. */
@Component
@ConditionalOnProperty(prefix = "parfait.canvas-cache", name = ["enabled"], havingValue = "false")
class NoOpCanvasCacheAdapter : CanvasCachePort {
    override fun getParfaitVersion(parfaitId: Long): Long? = null

    override fun putParfaitVersion(
        parfaitId: Long,
        version: Long,
    ) = Unit

    override fun evictParfaitVersion(parfaitId: Long) = Unit

    override fun getGroupVersion(groupId: Long): Long? = null

    override fun putGroupVersion(
        groupId: Long,
        version: Long,
    ) = Unit

    override fun evictGroupVersion(groupId: Long) = Unit

    override fun getTodayParfaitId(
        groupId: Long,
        date: LocalDate,
    ): Long? = null

    override fun putTodayParfaitId(
        groupId: Long,
        date: LocalDate,
        parfaitId: Long,
    ) = Unit

    override fun evictTodayParfaitId(
        groupId: Long,
        date: LocalDate,
    ) = Unit

    override fun isMember(
        groupId: Long,
        memberId: Long,
    ): Boolean = false

    override fun putMember(
        groupId: Long,
        memberId: Long,
    ) = Unit

    override fun evictMember(
        groupId: Long,
        memberId: Long,
    ) = Unit
}
