package parfait.core.parfait.port.out

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
}

data class ParfaitSummary(
    val id: Long,
    val date: LocalDate,
)
