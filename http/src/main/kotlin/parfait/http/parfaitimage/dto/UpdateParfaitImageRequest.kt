package parfait.http.parfaitimage.dto

import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageCommand

data class UpdateParfaitImageRequest(
    val positionX: Double?,
    val positionY: Double?,
    val positionZ: Int?,
    val scale: Double?,
    val rotation: Double?,
) {
    fun toCommand(
        memberId: Long,
        groupId: Long,
        parfaitId: Long,
        parfaitImageId: Long,
    ): UpdateParfaitImageCommand =
        UpdateParfaitImageCommand(
            memberId = memberId,
            groupId = groupId,
            parfaitId = parfaitId,
            parfaitImageId = parfaitImageId,
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            scale = scale,
            rotation = rotation,
        )
}
