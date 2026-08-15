package parfait.core.parfait.port.out

import parfait.core.parfait.domain.Parfait
import java.time.LocalDate

interface ParfaitQueryPort {
    fun findDistinctYearsByGroupId(groupId: Long): List<Int>

    fun existsByIdAndGroupId(
        parfaitId: Long,
        groupId: Long,
    ): Boolean

    fun findAllByGroupIdAndDateRange(
        groupId: Long,
        from: LocalDate,
        to: LocalDate,
    ): List<ParfaitSummary>

    fun findByGroupIdAndDate(
        groupId: Long,
        date: LocalDate,
    ): Parfait?

    fun findLastClosedDateByGroupId(groupId: Long): LocalDate?

    fun findByIdAndGroupId(
        parfaitId: Long,
        groupId: Long,
    ): Parfait?
}

data class ParfaitSummary(
    val id: Long,
    val date: LocalDate,
)
