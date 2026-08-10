package parfait.http.parfaitimage.dto

import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageCommand

data class PlaceParfaitImageRequest(
    val imageId: Long,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val borderType: BorderType,
    val borderColor: String?,
    val borderWidth: Double?,
) {
    fun toCommand(
        memberId: Long,
        groupId: Long,
        parfaitId: Long,
    ): PlaceParfaitImageCommand =
        PlaceParfaitImageCommand(
            memberId = memberId,
            groupId = groupId,
            parfaitId = parfaitId,
            imageId = imageId,
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            scale = scale,
            rotation = rotation,
            borderType = borderType,
            borderColor = borderColor,
            borderWidth = borderWidth,
        )
}
