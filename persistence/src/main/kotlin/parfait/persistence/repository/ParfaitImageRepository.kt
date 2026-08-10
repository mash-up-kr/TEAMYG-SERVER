package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.persistence.entity.ParfaitImage

interface ParfaitImageRepository : JpaRepository<ParfaitImage, Long> {
    fun findByParfaitIdAndImageMetaId(
        parfaitId: Long,
        imageMetaId: Long,
    ): ParfaitImage?
}
