package parfait.core.parfaitimage.port.out

import parfait.core.parfaitimage.domain.ParfaitImage

interface ParfaitImageQueryPort {
    fun findByParfaitIdAndImageMetaId(
        parfaitId: Long,
        imageMetaId: Long,
    ): ParfaitImage?
}
