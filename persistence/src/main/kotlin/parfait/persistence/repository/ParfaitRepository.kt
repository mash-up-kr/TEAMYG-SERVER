package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
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

    fun findByIdAndParfaitGroupId(
        id: Long,
        parfaitGroupId: Long,
    ): Parfait?

    @Query(value = "SELECT version FROM parfait WHERE id = :id", nativeQuery = true)
    fun findVersionById(
        @Param("id") id: Long,
    ): Long?

    @Modifying
    @Query(value = "UPDATE parfait SET version = version + 1 WHERE id = :id", nativeQuery = true)
    fun incrementVersion(
        @Param("id") id: Long,
    ): Int
}
