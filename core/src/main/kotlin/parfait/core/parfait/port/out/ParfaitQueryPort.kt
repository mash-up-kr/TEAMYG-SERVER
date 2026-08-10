package parfait.core.parfait.port.out

interface ParfaitQueryPort {
    fun findDistinctYearsByGroupId(groupId: Long): List<Int>

    fun existsByIdAndGroupId(
        parfaitId: Long,
        groupId: Long,
    ): Boolean
}
