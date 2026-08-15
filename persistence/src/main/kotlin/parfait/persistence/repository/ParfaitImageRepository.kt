package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import parfait.persistence.entity.ParfaitImage

interface ParfaitImageRepository : JpaRepository<ParfaitImage, Long> {
    fun findByParfaitIdAndImageMetaId(
        parfaitId: Long,
        imageMetaId: Long,
    ): ParfaitImage?

    fun findAllByParfaitId(parfaitId: Long): List<ParfaitImage>

    fun existsByParfaitId(parfaitId: Long): Boolean

    @Query(
        "SELECT pi.parfaitId AS parfaitId, COUNT(pi) AS count FROM ParfaitImage pi " +
            "WHERE pi.parfaitId IN :parfaitIds GROUP BY pi.parfaitId",
    )
    fun countAllByParfaitIdIn(
        @Param("parfaitIds") parfaitIds: Collection<Long>,
    ): List<ParfaitImageCountProjection>
}

interface ParfaitImageCountProjection {
    val parfaitId: Long
    val count: Long
}
