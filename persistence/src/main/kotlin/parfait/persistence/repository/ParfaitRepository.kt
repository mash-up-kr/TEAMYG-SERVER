package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import parfait.persistence.entity.Parfait
import java.time.LocalDate

interface ParfaitRepository : JpaRepository<Parfait, Long> {
    @Query(
        "SELECT DISTINCT YEAR(p.parfaitDate) FROM Parfait p WHERE p.parfaitGroupId = :groupId ORDER BY YEAR(p.parfaitDate) ASC",
    )
    fun findDistinctYearsByParfaitGroupId(
        @Param("groupId") groupId: Long,
    ): List<Int>

    fun existsByIdAndParfaitGroupId(
        id: Long,
        parfaitGroupId: Long,
    ): Boolean

    fun findAllByParfaitGroupIdAndParfaitDateBetweenOrderByParfaitDateDesc(
        parfaitGroupId: Long,
        from: LocalDate,
        to: LocalDate,
    ): List<Parfait>

    fun findByParfaitGroupIdAndParfaitDate(
        parfaitGroupId: Long,
        parfaitDate: LocalDate,
    ): Parfait?

    fun findTopByParfaitGroupIdAndStatusOrderByParfaitDateDesc(
        parfaitGroupId: Long,
        status: String,
    ): Parfait?
}
