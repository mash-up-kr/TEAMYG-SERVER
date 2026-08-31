package parfait.core.parfaitimage.port.out

import parfait.core.parfaitimage.domain.ParfaitImage

interface ParfaitImageQueryPort {
    fun findByParfaitIdAndImageMetaId(
        parfaitId: Long,
        imageMetaId: Long,
    ): ParfaitImage?

    fun findById(parfaitImageId: Long): ParfaitImage?

    fun findAllByIds(parfaitImageIds: Collection<Long>): List<ParfaitImage>

    fun countAllByParfaitIds(parfaitIds: Collection<Long>): Map<Long, Int>

    fun findAllByParfaitId(parfaitId: Long): List<ParfaitImage>

    fun existsByParfaitId(parfaitId: Long): Boolean
}
