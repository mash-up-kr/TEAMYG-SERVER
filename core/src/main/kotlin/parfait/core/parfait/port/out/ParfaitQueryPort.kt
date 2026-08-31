package parfait.core.parfait.port.out

import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import java.time.LocalDate

interface ParfaitQueryPort {
    fun findDistinctYearsByGroupId(groupId: Long): List<Int>

    fun findAllByGroupIdAndDateRange(
        groupId: Long,
        from: LocalDate,
        to: LocalDate,
    ): List<ParfaitSummary>

    fun findByGroupIdAndDate(
        groupId: Long,
        date: LocalDate,
    ): Parfait?

    /**
     * 그룹당 ACTIVE 상태의 파르페는 최대 1개라는 가정에 의존한다(DB 제약 없음,
     * 캔버스 회전 배치 로직이 이 불변식을 유지한다).
     */
    fun findActiveByGroupId(groupId: Long): Parfait?

    fun findLastClosedDateByGroupId(groupId: Long): LocalDate?

    fun findByIdAndGroupId(
        parfaitId: Long,
        groupId: Long,
    ): Parfait?
}

data class ParfaitSummary(
    val id: Long,
    val date: LocalDate,
    val status: ParfaitStatus,
)
